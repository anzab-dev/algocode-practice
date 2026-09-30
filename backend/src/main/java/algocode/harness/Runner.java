package algocode.harness;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the sandbox JVM.
 * <p>
 * Reads a job description (JSON) from the file given as the first argument, runs the user's
 * code and prints exactly one result line to the real stdout, prefixed with a per-job nonce so
 * the backend can tell it apart from anything the user's code prints. The nonce is read from
 * the job file before any user code runs.
 * <p>
 * Two modes:
 * <ul>
 *   <li>{@code solve}: instantiate {@code className} for every test, call {@code method} with the
 *   converted arguments and report the serialized return value, per-test time and output.</li>
 *   <li>{@code scratch}: run {@code mainClass.main} with the given stdin, capturing stdout/stderr.</li>
 * </ul>
 */
public final class Runner {

    static final int TEST_OUTPUT_LIMIT = 8 * 1024;
    static final int SCRATCH_OUTPUT_LIMIT = 64 * 1024;
    static final long WORKER_STACK_BYTES = 256L * 1024 * 1024;

    private final PrintStream realOut;
    private final String nonce;
    private final Map<String, Object> result = new LinkedHashMap<>();
    private boolean emitted;

    private Runner(PrintStream realOut, String nonce) {
        this.realOut = realOut;
        this.nonce = nonce;
    }

    public static void main(String[] args) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) Json.parse(Files.readString(Path.of(args[0])));
        Runner runner = new Runner(System.out, (String) job.get("nonce"));
        Runtime.getRuntime().addShutdownHook(new Thread(runner::emitOnExit));
        runner.run(job);
        runner.emit();
        runner.realOut.flush();
        // User code may have left non-daemon threads behind.
        Runtime.getRuntime().halt(0);
    }

    private void run(Map<String, Object> job) throws InterruptedException {
        String mode = (String) job.get("mode");
        long timeLimitMs = ((Number) job.getOrDefault("timeLimitMs", 2000L)).longValue();
        result.put("status", "OK");

        Body body = "scratch".equals(mode) ? () -> runScratch(job) : () -> runSolve(job);
        Thread worker = new Thread(null, () -> {
            try {
                body.run();
            } catch (OutOfMemoryError oom) {
                synchronized (this) {
                    result.put("status", "MEMORY_LIMIT");
                    result.put("error", "java.lang.OutOfMemoryError: " + oom.getMessage());
                }
            } catch (Throwable t) {
                synchronized (this) {
                    result.put("status", "SETUP_ERROR");
                    result.put("error", t.toString());
                }
            }
        }, "solution", WORKER_STACK_BYTES);
        worker.setDaemon(true);
        worker.start();
        worker.join(timeLimitMs);
        if (worker.isAlive()) {
            synchronized (this) {
                result.put("status", "TIME_LIMIT");
            }
        }
    }

    @FunctionalInterface
    private interface Body {
        void run() throws Exception;
    }

    // ---------------------------------------------------------------- solve mode

    private void runSolve(Map<String, Object> job) throws Exception {
        String className = (String) job.getOrDefault("className", "Solution");
        String methodName = (String) job.get("method");
        List<?> tests = (List<?>) job.get("tests");
        boolean stopOnError = Boolean.TRUE.equals(job.getOrDefault("stopOnError", Boolean.TRUE));

        Class<?> cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail("Class '" + className + "' was not found. Keep the class name from the starter code.");
            return;
        }
        List<Object> results = new ArrayList<>();
        synchronized (this) {
            result.put("tests", results);
        }

        CaptureStream capture = new CaptureStream(TEST_OUTPUT_LIMIT);
        PrintStream captured = new PrintStream(capture, true, StandardCharsets.UTF_8);
        System.setOut(captured);
        System.setErr(captured);
        System.setIn(new ByteArrayInputStream(new byte[0]));

        MemoryProbe probe = new MemoryProbe();
        long totalNs = 0;
        for (Object testArgs : tests) {
            List<?> args = (List<?>) testArgs;
            Method method = findMethod(cls, methodName, args.size());
            if (method == null) {
                fail("Method '" + methodName + "' taking " + args.size()
                        + " argument(s) was not found in class " + className + ".");
                return;
            }
            Map<String, Object> test = new LinkedHashMap<>();
            Object[] converted = convertArgs(method, args);
            Object instance = Modifier.isStatic(method.getModifiers())
                    ? null : newInstance(cls);
            long start = System.nanoTime();
            Object returned = null;
            Throwable thrown = null;
            try {
                returned = method.invoke(instance, converted);
            } catch (InvocationTargetException e) {
                thrown = e.getCause();
            }
            long elapsed = System.nanoTime() - start;
            totalNs += elapsed;
            probe.sample();

            test.put("timeNs", elapsed);
            if (thrown instanceof OutOfMemoryError oom) {
                throw oom;
            }
            if (thrown != null) {
                test.put("error", describe(thrown));
            } else if (method.getReturnType() == void.class && converted.length > 0) {
                // In-place problems: the (mutated) first argument is the answer.
                test.put("output", Json.parse(Json.stringify(converted[0])));
            } else {
                test.put("output", Json.parse(Json.stringify(returned)));
            }
            test.put("stdout", capture.drain());
            synchronized (this) {
                results.add(test);
                result.put("timeNs", totalNs);
                probe.writeTo(result);
            }
            if (thrown != null && stopOnError) {
                break;
            }
        }
    }

    private static Method findMethod(Class<?> cls, String name, int arity) {
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == arity && !m.isSynthetic()) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static Object[] convertArgs(Method method, List<?> args) {
        Type[] types = method.getGenericParameterTypes();
        Object[] out = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = ArgConverter.convert(args.get(i), types[i]);
        }
        return out;
    }

    private static Object newInstance(Class<?> cls) throws ReflectiveOperationException {
        var ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    // ---------------------------------------------------------------- scratch mode

    private void runScratch(Map<String, Object> job) throws Exception {
        String mainClass = (String) job.get("mainClass");
        String stdin = (String) job.getOrDefault("stdin", "");

        Method main;
        try {
            main = Class.forName(mainClass).getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("No 'public static void main(String[] args)' found in class " + mainClass + ".");
            return;
        }
        CaptureStream out = new CaptureStream(SCRATCH_OUTPUT_LIMIT);
        CaptureStream err = new CaptureStream(SCRATCH_OUTPUT_LIMIT);
        synchronized (this) {
            result.put("stdoutCapture", out);
            result.put("stderrCapture", err);
        }
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));

        MemoryProbe probe = new MemoryProbe();
        long start = System.nanoTime();
        try {
            main.invoke(null, (Object) new String[0]);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof OutOfMemoryError oom) {
                throw oom;
            }
            synchronized (this) {
                result.put("exception", describe(e.getCause()));
            }
        } finally {
            long elapsed = System.nanoTime() - start;
            probe.sample();
            synchronized (this) {
                result.put("timeNs", elapsed);
                probe.writeTo(result);
            }
        }
    }

    // ---------------------------------------------------------------- reporting

    private synchronized void fail(String message) {
        result.put("status", "SETUP_ERROR");
        result.put("error", message);
    }

    private static Map<String, Object> describe(Throwable t) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("type", t.getClass().getName());
        error.put("message", t.getMessage());
        StringBuilder trace = new StringBuilder(t.toString());
        int frames = 0;
        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            if (cls.startsWith("algocode.harness.") || cls.startsWith("jdk.internal.reflect.")
                    || cls.startsWith("java.lang.reflect.")) {
                break;
            }
            trace.append("\n\tat ").append(frame);
            if (++frames >= 12) {
                trace.append("\n\t...");
                break;
            }
        }
        error.put("trace", trace.toString());
        return error;
    }

    private void emitOnExit() {
        synchronized (this) {
            if (!emitted && "OK".equals(result.get("status"))) {
                result.put("status", "EXITED");
            }
        }
        emit();
    }

    private synchronized void emit() {
        if (emitted) {
            return;
        }
        emitted = true;
        Map<String, Object> out = new LinkedHashMap<>(result);
        if (out.remove("stdoutCapture") instanceof CaptureStream stdout) {
            out.put("stdout", stdout.drain());
            out.put("stdoutTruncated", stdout.truncated());
        }
        if (out.remove("stderrCapture") instanceof CaptureStream stderr) {
            out.put("stderr", stderr.drain());
        }
        realOut.print(nonce + Json.stringify(out) + "\n");
        realOut.flush();
    }

    /** Tracks peak heap usage above the baseline taken when the probe is created. */
    static final class MemoryProbe {
        private final List<MemoryPoolMXBean> pools = new ArrayList<>();
        private final long baseline;
        private final long startAllocated;
        private long peak;

        MemoryProbe() {
            System.gc();
            long used = 0;
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getType() == MemoryType.HEAP && pool.isValid()) {
                    pool.resetPeakUsage();
                    pools.add(pool);
                    used += pool.getUsage().getUsed();
                }
            }
            baseline = used;
            startAllocated = allocatedBytes();
        }

        void sample() {
            long sum = 0;
            for (MemoryPoolMXBean pool : pools) {
                sum += pool.getPeakUsage().getUsed();
            }
            peak = Math.max(peak, sum);
        }

        void writeTo(Map<String, Object> result) {
            result.put("peakHeapBytes", Math.max(0, peak - baseline));
            result.put("allocatedBytes", Math.max(0, allocatedBytes() - startAllocated));
        }

        private static long allocatedBytes() {
            if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean) {
                return bean.getCurrentThreadAllocatedBytes();
            }
            return 0;
        }
    }
}
