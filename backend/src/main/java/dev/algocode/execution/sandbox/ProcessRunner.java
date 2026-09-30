package dev.algocode.execution.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Runs an OS process with a wall-clock limit while draining its output with a size cap. */
final class ProcessRunner {

    private ProcessRunner() {
    }

    interface OnTimeout {
        void kill(Process process);
    }

    static SandboxOutput run(ProcessBuilder builder, byte[] stdin, Duration timeout, int maxOutputBytes,
                             OnTimeout onTimeout) {
        long started = System.nanoTime();
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start sandbox process: " + builder.command().getFirst(), e);
        }
        CompletableFuture<String> out = drain(process.getInputStream(), maxOutputBytes);
        CompletableFuture<String> err = drain(process.getErrorStream(), maxOutputBytes);
        CompletableFuture.runAsync(() -> {
            try (OutputStream in = process.getOutputStream()) {
                if (stdin != null) {
                    in.write(stdin);
                }
            } catch (IOException ignored) {
                // process died before reading its input; the exit code tells the story
            }
        });
        boolean killed = false;
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                killed = true;
                onTimeout.kill(process);
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            killed = true;
        }
        long wallMs = (System.nanoTime() - started) / 1_000_000;
        int exit = process.isAlive() ? -1 : process.exitValue();
        return new SandboxOutput(exit, out.join(), err.join(), killed, wallMs);
    }

    private static CompletableFuture<String> drain(InputStream stream, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            try (stream) {
                int n;
                while ((n = stream.read(chunk)) != -1) {
                    int room = limit - buffer.size();
                    if (room > 0) {
                        buffer.write(chunk, 0, Math.min(n, room));
                    }
                }
            } catch (IOException ignored) {
                // stream closed because the process was killed
            }
            return buffer.toString(StandardCharsets.UTF_8);
        });
    }
}
