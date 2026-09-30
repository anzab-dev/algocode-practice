package dev.algopractice.execution;

import java.util.List;

/** Result of running a solution class against a list of argument sets. */
public record SolveReport(
        RunStatus status,
        String message,
        List<SourceDiagnostic> diagnostics,
        List<TestRun> tests,
        long totalTimeNanos,
        long peakHeapBytes,
        long allocatedBytes) {
}
