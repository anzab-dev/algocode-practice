package dev.algopractice.execution.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import dev.algopractice.execution.ExecutionService;
import dev.algopractice.execution.HarnessBundle;
import dev.algopractice.execution.JavaSourceCompiler;
import dev.algopractice.execution.RunStatus;
import dev.algopractice.execution.ScratchReport;
import dev.algopractice.execution.SolveReport;
import dev.algopractice.telemetry.MicrometerTelemetry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import tools.jackson.databind.json.JsonMapper;

/** Runs real containers; skipped where no Docker daemon is reachable. */
@EnabledIf("dockerAvailable")
class DockerSandboxIntegrationTest {

    private static ExecutionService execution;
    private final JsonMapper json = JsonMapper.builder().build();

    static boolean dockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void setUp() throws Exception {
        SandboxProperties properties = new SandboxProperties();
        properties.setMode(SandboxProperties.Mode.DOCKER);
        properties.setStartupGraceMs(60_000); // first run may pull the image
        new ProcessBuilder("docker", "pull", properties.getDockerImage()).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
        execution = new ExecutionService(new JavaSourceCompiler(), new HarnessBundle(),
                new DockerSandboxExecutor(properties), properties,
                new MicrometerTelemetry(new SimpleMeterRegistry(), ObservationRegistry.NOOP),
                JsonMapper.builder().build(), 5000);
    }

    @Test
    void judgesInsideAContainer() {
        SolveReport report = execution.solve("class Solution { public int add(int a, int b) { return a + b; } }",
                "Solution", "add", List.of(json.readTree("[2, 3]")), 2000, true);
        assertThat(report.status()).as(report.message()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.tests().getFirst().output().asInt()).isEqualTo(5);
    }

    @Test
    void containerHasNoNetworkAndReadOnlyRoot() {
        String code = """
                import java.net.*;
                import java.nio.file.*;
                public class Main {
                    public static void main(String[] args) throws Exception {
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
                            System.out.println("network: open");
                        } catch (Exception e) {
                            System.out.println("network: blocked");
                        }
                        try {
                            Files.writeString(Path.of("/etc/pwned"), "x");
                            System.out.println("rootfs: writable");
                        } catch (Exception e) {
                            System.out.println("rootfs: read-only");
                        }
                    }
                }
                """;
        ScratchReport report = execution.scratch(code, "");
        assertThat(report.status()).as(report.message()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.stdout()).contains("network: blocked").contains("rootfs: read-only");
    }

    @Test
    void killsRunawayContainers() {
        ScratchReport report = execution.scratch(
                "public class Main { public static void main(String[] a) { while (true) { } } }", "");
        assertThat(report.status()).isEqualTo(RunStatus.TIME_LIMIT_EXCEEDED);
    }
}
