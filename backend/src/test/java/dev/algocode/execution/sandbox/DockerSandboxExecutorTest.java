package dev.algocode.execution.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

class DockerSandboxExecutorTest {

    private final SandboxJob job = new SandboxJob(Map.of("Solution", new byte[] {1, 2}, "algocode.harness.Runner",
            new byte[] {3}), "{\"nonce\":\"n\"}", "algocode.harness.Runner", Duration.ofSeconds(5));

    @Test
    void commandLocksTheContainerDown() {
        List<String> cmd = new DockerSandboxExecutor(new SandboxProperties()).command("c1", job);
        String joined = String.join(" ", cmd);
        assertThat(joined).contains("--network none", "--read-only", "--cap-drop ALL", "--pids-limit 64",
                "--memory 512m", "--memory-swap 512m", "--user 65534:65534", "no-new-privileges", "--rm", "-i");
        assertThat(cmd.getLast()).startsWith("tar -x -C /sandbox && exec java -Xmx256m")
                .endsWith("algocode.harness.Runner /sandbox/job.json");
    }

    @Test
    void tarballContainsClassFilesAndJob() throws Exception {
        List<String> names = new ArrayList<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new ByteArrayInputStream(DockerSandboxExecutor.tarball(job)))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        assertThat(names).containsExactlyInAnyOrder("Solution.class", "algocode/harness/Runner.class", "job.json");
    }
}
