package dev.algocode.execution.sandbox;

/** Runs compiled code somewhere isolated from the backend and returns its raw output. */
public interface SandboxExecutor {

    SandboxOutput execute(SandboxJob job);

    /** Short name for logs and metrics, e.g. {@code docker}. */
    String name();
}
