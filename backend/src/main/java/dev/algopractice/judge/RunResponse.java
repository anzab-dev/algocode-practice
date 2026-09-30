package dev.algopractice.judge;

import dev.algopractice.execution.SourceDiagnostic;
import dev.algopractice.submission.Verdict;
import java.util.List;

/** Result of "Run": sample and custom tests only, nothing is recorded. */
public record RunResponse(
        Verdict verdict,
        String message,
        List<SourceDiagnostic> diagnostics,
        List<CaseResult> cases,
        double runtimeMs,
        long memoryBytes) {
}
