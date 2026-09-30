package dev.algopractice.execution;

import java.util.List;

/** Result of running a scratchpad program. */
public record ScratchReport(
        RunStatus status,
        String message,
        List<SourceDiagnostic> diagnostics,
        String stdout,
        String stderr,
        boolean stdoutTruncated,
        TestRun.RuntimeFailure exception,
        double timeMs,
        long peakHeapBytes,
        long allocatedBytes,
        String mainClass) {
}
