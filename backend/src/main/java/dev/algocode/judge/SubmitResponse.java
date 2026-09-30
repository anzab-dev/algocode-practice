package dev.algocode.judge;

import dev.algocode.execution.SourceDiagnostic;
import dev.algocode.progress.Achievement;
import dev.algocode.submission.Verdict;
import java.util.List;

/** Result of "Submit": every test, recorded, with percentiles and progress changes. */
public record SubmitResponse(
        long submissionId,
        Verdict verdict,
        String message,
        List<SourceDiagnostic> diagnostics,
        int passed,
        int total,
        Double runtimeMs,
        Long memoryBytes,
        Long allocatedBytes,
        Double runtimeBeats,
        Double memoryBeats,
        CaseResult failedCase,
        int xpGained,
        int level,
        int streak,
        List<Achievement> newAchievements) {
}
