package dev.algopractice.execution.sandbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Runs the harness in a child JVM on the backend host.
 * <p>
 * There is no isolation beyond a separate process with a heap cap and a kill timer, so this
 * mode is for local development and tests only. Use {@link DockerSandboxExecutor} anywhere
 * untrusted people can submit code.
 */
public class LocalProcessSandboxExecutor implements SandboxExecutor {

    private final SandboxProperties properties;

    public LocalProcessSandboxExecutor(SandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public SandboxOutput execute(SandboxJob job) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("algopractice-");
            writeClassFiles(dir, job.classFiles());
            Path jobFile = dir.resolve(SandboxJob.JOB_FILE);
            Files.writeString(jobFile, job.jobJson(), StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>();
            command.add(properties.getJavaBinary());
            command.addAll(properties.jvmFlags());
            command.addAll(List.of("-cp", dir.toString(), job.mainClass(), jobFile.toString()));
            ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile());
            // Do not leak the backend's environment (credentials, JAVA_TOOL_OPTIONS, ...).
            builder.environment().clear();
            return ProcessRunner.run(builder, null, job.hardTimeout(), properties.getMaxOutputBytes(),
                    process -> process.descendants().forEach(ProcessHandle::destroyForcibly));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(dir);
        }
    }

    static void writeClassFiles(Path root, Map<String, byte[]> classFiles) throws IOException {
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            Path file = root.resolve(entry.getKey().replace('.', '/') + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, entry.getValue());
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // temp directory; the OS will clean it eventually
        }
    }
}
