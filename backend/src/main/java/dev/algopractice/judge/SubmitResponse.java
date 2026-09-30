package dev.algopractice.judge;

import dev.algopractice.execution.SourceDiagnostic;
import dev.algopractice.progress.Achievement;
import dev.algopractice.submission.Verdict;
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
