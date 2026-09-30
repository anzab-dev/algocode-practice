package dev.algocode.execution.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * Runs each job in a fresh, locked-down container: no network, read-only root file system,
 * all capabilities dropped, an unprivileged user, and CPU, memory and process limits.
 * <p>
 * Class files and the job description are streamed in on stdin as a tar archive and unpacked
 * into a tmpfs, so no host directory is shared. That also means the backend itself can run in
 * a container and talk to the host's Docker daemon without any path mapping.
 */
public class DockerSandboxExecutor implements SandboxExecutor {

    static final String WORKDIR = "/sandbox";

    private final SandboxProperties properties;

    public DockerSandboxExecutor(SandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "docker";
    }

    @Override
    public SandboxOutput execute(SandboxJob job) {
        String containerName = "algocode-" + UUID.randomUUID();
        ProcessBuilder builder = new ProcessBuilder(command(containerName, job));
        return ProcessRunner.run(builder, tarball(job), job.hardTimeout(), properties.getMaxOutputBytes(),
                process -> killContainer(containerName));
    }

    List<String> command(String containerName, SandboxJob job) {
        List<String> cmd = new ArrayList<>(List.of(
                properties.getDockerBinary(), "run", "--rm", "-i",
                "--name", containerName,
                "--network", "none",
                "--read-only",
                "--tmpfs", WORKDIR + ":rw,exec,nosuid,size=64m,mode=1777",
                "--tmpfs", "/tmp:rw,nosuid,size=16m,mode=1777",
                "--memory", properties.getContainerMemoryMb() + "m",
                "--memory-swap", properties.getContainerMemoryMb() + "m",
                "--cpus", String.format(Locale.ROOT, "%.2f", properties.getCpus()),
                "--pids-limit", String.valueOf(properties.getPidsLimit()),
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "--user", "65534:65534",
                "--workdir", WORKDIR,
                "--entrypoint", "sh",
                properties.getDockerImage(),
                "-c"));
        String java = "exec java " + String.join(" ", properties.jvmFlags())
                + " -cp " + WORKDIR + " " + job.mainClass() + " " + WORKDIR + "/" + SandboxJob.JOB_FILE;
        cmd.add("tar -x -C " + WORKDIR + " && " + java);
        return cmd;
    }

    static byte[] tarball(SandboxJob job) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, byte[]> entry : job.classFiles().entrySet()) {
                addEntry(tar, entry.getKey().replace('.', '/') + ".class", entry.getValue());
            }
            addEntry(tar, SandboxJob.JOB_FILE, job.jobJson().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void addEntry(TarArchiveOutputStream tar, String name, byte[] content) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(content.length);
        entry.setMode(0644);
        tar.putArchiveEntry(entry);
        tar.write(content);
        tar.closeArchiveEntry();
    }

    private void killContainer(String containerName) {
        try {
            new ProcessBuilder(properties.getDockerBinary(), "kill", containerName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            // container may already be gone
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
