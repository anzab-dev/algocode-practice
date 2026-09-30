package dev.algocode.execution.sandbox;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * In DOCKER mode, pulls the sandbox image in the background at start-up so the first
 * submission is not charged for the download. Retries while the Docker daemon comes up,
 * which matters when it is a sidecar (Kubernetes) that starts alongside the backend.
 */
@Component
class SandboxImageWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxImageWarmup.class);
    static final int ATTEMPTS = 30;

    private final SandboxProperties properties;

    SandboxImageWarmup(SandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getMode() != SandboxProperties.Mode.DOCKER) {
            return;
        }
        Thread.ofVirtual().name("sandbox-image-warmup").start(this::warmUp);
    }

    private void warmUp() {
        String image = properties.getDockerImage();
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            if (docker(60, "image", "inspect", image) || docker(600, "pull", image)) {
                log.info("Sandbox image {} is ready", image);
                return;
            }
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Could not pull sandbox image {}; the first run will try again", image);
    }

    private boolean docker(long timeoutSeconds, String... args) {
        String[] command = new String[args.length + 1];
        command[0] = properties.getDockerBinary();
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getInputStream().transferTo(OutputStream.nullOutputStream());
            return process.waitFor(timeoutSeconds, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
