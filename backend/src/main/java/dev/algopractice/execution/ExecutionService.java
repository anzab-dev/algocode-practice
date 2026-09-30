package dev.algopractice.execution;

import dev.algopractice.execution.sandbox.SandboxExecutor;
import dev.algopractice.execution.sandbox.SandboxJob;
import dev.algopractice.execution.sandbox.SandboxOutput;
import dev.algopractice.execution.sandbox.SandboxProperties;
import dev.algopractice.telemetry.AlgoTelemetry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Compiles user code, ships it to the sandbox together with the harness and turns the
 * harness's report into a {@link SolveReport} or {@link ScratchReport}.
 */
@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final JavaSourceCompiler compiler;
    private final HarnessBundle harness;
    private final SandboxExecutor sandbox;
    private final SandboxProperties properties;
    private final AlgoTelemetry telemetry;
    private final ObjectMapper json;
    private final Semaphore slots;
    private final long scratchTimeLimitMs;

    public ExecutionService(JavaSourceCompiler compiler, HarnessBundle harness, SandboxExecutor sandbox,
                            SandboxProperties properties, AlgoTelemetry telemetry, ObjectMapper json,
                            @Value("${algopractice.scratch.time-limit-ms:5000}") long scratchTimeLimitMs) {
        this.compiler = compiler;
        this.harness = harness;
        this.sandbox = sandbox;
        this.properties = properties;
        this.telemetry = telemetry;
        this.json = json;
        this.slots = new Semaphore(Math.max(1, properties.getMaxConcurrent()), true);
        this.scratchTimeLimitMs = scratchTimeLimitMs;
    }

    /**
     * Runs {@code method} of {@code className} once per argument list.
     *
     * @param stopOnError stop at the first test that throws (judge mode) or run them all
     */
    public SolveReport solve(String code, String className, String method, List<JsonNode> argumentLists,
                             long timeLimitMs, boolean stopOnError) {
        return telemetry.observe("algopractice.solve", Map.of("sandbox", sandbox.name()), () -> {
            long compileStart = System.nanoTime();
            CompilationResult compiled = compiler.compile(code, className);
            long compileNanos = System.nanoTime() - compileStart;
            if (!compiled.success()) {
                report("solve", RunStatus.COMPILE_ERROR, compileNanos, 0);
                return new SolveReport(RunStatus.COMPILE_ERROR, firstError(compiled), compiled.diagnostics(),
                        List.of(), 0, 0, 0);
            }
            ObjectNode job = newJob("solve", timeLimitMs);
            job.put("className", className);
            job.put("method", method);
            job.put("stopOnError", stopOnError);
            job.putArray("tests").addAll(argumentLists);

            SandboxOutput output = runInSandbox(compiled, job, timeLimitMs);
            HarnessResult result = interpret(output, job.get("nonce").asString());
            report("solve", result.status, compileNanos, output.wallTimeMs());

            List<TestRun> tests = new ArrayList<>();
            JsonNode testsNode = result.body == null ? null : result.body.get("tests");
            if (testsNode != null) {
                for (JsonNode t : testsNode) {
                    tests.add(new TestRun(t.has("output") ? t.get("output") : null,
                            text(t, "stdout"), t.path("timeNs").asLong(), failure(t.get("error"))));
                }
            }
            return new SolveReport(result.status, result.message, compiled.diagnostics(), tests,
                    longField(result, "timeNs"), longField(result, "peakHeapBytes"),
                    longField(result, "allocatedBytes"));
        });
    }

    /** Runs a stand-alone program (a class with {@code main}) with the given stdin. */
    public ScratchReport scratch(String code, String stdin) {
        return telemetry.observe("algopractice.scratch", Map.of("sandbox", sandbox.name()), () -> {
            long compileStart = System.nanoTime();
            CompilationResult compiled = compiler.compile(code, "Main");
            long compileNanos = System.nanoTime() - compileStart;
            if (!compiled.success()) {
                report("scratch", RunStatus.COMPILE_ERROR, compileNanos, 0);
                return new ScratchReport(RunStatus.COMPILE_ERROR, firstError(compiled), compiled.diagnostics(),
                        "", "", false, null, 0, 0, 0, null);
            }
            if (compiled.mainClasses().isEmpty()) {
                return new ScratchReport(RunStatus.SETUP_ERROR,
                        "Add a 'public static void main(String[] args)' method to run the scratchpad.",
                        compiled.diagnostics(), "", "", false, null, 0, 0, 0, null);
            }
            String mainClass = compiled.mainClasses().getFirst();
            ObjectNode job = newJob("scratch", scratchTimeLimitMs);
            job.put("mainClass", mainClass);
            job.put("stdin", stdin == null ? "" : stdin);

            SandboxOutput output = runInSandbox(compiled, job, scratchTimeLimitMs);
            HarnessResult result = interpret(output, job.get("nonce").asString());
            report("scratch", result.status, compileNanos, output.wallTimeMs());
            JsonNode body = result.body;
            return new ScratchReport(result.status, result.message, compiled.diagnostics(),
                    body == null ? "" : text(body, "stdout"),
                    body == null ? output.stderr() : text(body, "stderr"),
                    body != null && body.path("stdoutTruncated").asBoolean(),
                    body == null ? null : failure(body.get("exception")),
                    longField(result, "timeNs") / 1_000_000.0,
                    longField(result, "peakHeapBytes"), longField(result, "allocatedBytes"), mainClass);
        });
    }

    private ObjectNode newJob(String mode, long timeLimitMs) {
        ObjectNode job = json.createObjectNode();
        job.put("nonce", "@@ALGOPRACTICE-" + UUID.randomUUID().toString().replace("-", "") + "@@");
        job.put("mode", mode);
        job.put("timeLimitMs", timeLimitMs);
        return job;
    }

    private SandboxOutput runInSandbox(CompilationResult compiled, ObjectNode job, long timeLimitMs) {
        Map<String, byte[]> classes = new HashMap<>(compiled.classFiles());
        classes.putAll(harness.classFiles());
        SandboxJob sandboxJob = new SandboxJob(classes, json.writeValueAsString(job), HarnessBundle.MAIN_CLASS,
                Duration.ofMillis(timeLimitMs + properties.getStartupGraceMs()));
        try {
            slots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a sandbox slot", e);
        }
        try {
            return sandbox.execute(sandboxJob);
        } finally {
            slots.release();
        }
    }

    private record HarnessResult(RunStatus status, String message, JsonNode body) {
    }

    private HarnessResult interpret(SandboxOutput output, String nonce) {
        int idx = output.stdout().lastIndexOf(nonce);
        if (idx >= 0) {
            String line = output.stdout().substring(idx + nonce.length()).lines().findFirst().orElse("");
            JsonNode body;
            try {
                body = json.readTree(line);
            } catch (RuntimeException e) {
                log.warn("Unreadable harness report: {}", e.getMessage());
                return new HarnessResult(RunStatus.INTERNAL_ERROR, "The sandbox returned an unreadable report.", null);
            }
            String status = body.path("status").asString("OK");
            return switch (status) {
                case "TIME_LIMIT" -> new HarnessResult(RunStatus.TIME_LIMIT_EXCEEDED, "Time limit exceeded", body);
                case "MEMORY_LIMIT" -> new HarnessResult(RunStatus.MEMORY_LIMIT_EXCEEDED, text(body, "error"), body);
                case "SETUP_ERROR" -> new HarnessResult(RunStatus.SETUP_ERROR, text(body, "error"), body);
                case "EXITED" -> new HarnessResult(RunStatus.EXITED, "The program called System.exit()", body);
                default -> new HarnessResult(RunStatus.COMPLETED, null, body);
            };
        }
        if (output.killed()) {
            return new HarnessResult(RunStatus.TIME_LIMIT_EXCEEDED, "Time limit exceeded", null);
        }
        if (output.stderr().contains("OutOfMemoryError") || output.exitCode() == 137) {
            return new HarnessResult(RunStatus.MEMORY_LIMIT_EXCEEDED, "Memory limit exceeded", null);
        }
        log.warn("Sandbox ended without a report (exit {}): {}", output.exitCode(), tail(output.stderr()));
        return new HarnessResult(RunStatus.INTERNAL_ERROR,
                "The sandbox ended unexpectedly (exit code " + output.exitCode() + "). " + tail(output.stderr()), null);
    }

    private void report(String kind, RunStatus status, long compileNanos, long wallMillis) {
        telemetry.executionFinished(new AlgoTelemetry.ExecutionEvent(kind, sandbox.name(), status.name(),
                compileNanos, wallMillis));
    }

    private static String firstError(CompilationResult compiled) {
        return compiled.errors().stream()
                .findFirst()
                .map(d -> "Line " + d.startLine() + ": " + d.message())
                .orElse("Compilation failed");
    }

    private static TestRun.RuntimeFailure failure(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new TestRun.RuntimeFailure(text(node, "type"), text(node, "message"), text(node, "trace"));
    }

    private static long longField(HarnessResult result, String field) {
        return result.body == null ? 0 : result.body.path(field).asLong();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String tail(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 2000 ? s.strip() : "..." + s.substring(s.length() - 2000).strip();
    }
}
