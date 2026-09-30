package dev.algopractice.progress;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything the dashboard shows about one user. */
public record Profile(
        String handle,
        int xp,
        int level,
        int levelStartXp,
        int nextLevelXp,
        int currentStreak,
        int longestStreak,
        int solved,
        int totalProblems,
        Map<String, DifficultyProgress> byDifficulty,
        int submissions,
        double acceptanceRate,
        List<AchievementStatus> achievements,
        Map<LocalDate, Integer> activity,
        Set<Long> solvedProblemIds,
        Set<Long> attemptedProblemIds) {

    public record DifficultyProgress(int solved, int total) {
    }

    public record AchievementStatus(Achievement achievement, boolean unlocked) {
    }
}
