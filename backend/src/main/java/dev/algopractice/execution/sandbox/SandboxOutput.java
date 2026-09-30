package dev.algopractice.execution.sandbox;

/** Raw result of a sandbox process. Streams are truncated to the configured limit. */
public record SandboxOutput(int exitCode, String stdout, String stderr, boolean killed, long wallTimeMs) {
}
