package dev.algocode.problem;

import java.time.Instant;
import java.util.List;

/** Read models returned by the problem API. */
public final class ProblemViews {

    private ProblemViews() {
    }

    public enum Status { SOLVED, ATTEMPTED }

    public record Summary(long id, String slug, String title, Difficulty difficulty, List<String> tags,
                          double acceptanceRate, int submissions, Status status) {
    }

    public record Sample(String input, String expected) {
    }

    public record Detail(long id, String slug, String title, Difficulty difficulty, List<String> tags,
                         String description, String starterCode, String method,
                         List<ProblemDefinition.Param> params, String returnType, CompareMode compareMode,
                         int timeLimitMs, List<String> hints, List<Sample> samples,
                         String previousSlug, String nextSlug) {
    }

    public record SubmissionSummary(long id, String problemSlug, String problemTitle, String verdict,
                                    int passed, int total, Double runtimeMs, Long memoryBytes,
                                    Double runtimeBeats, Double memoryBeats, Instant createdAt) {
    }

    public record SubmissionDetail(SubmissionSummary summary, String code, String message, String failedTestJson) {
    }

    /** Histogram of accepted results; {@code mine} marks the caller's best. */
    public record Distribution(List<Bucket> buckets, Double mine, int count) {
    }

    public record Bucket(double from, double to, int count) {
    }

    public record Stats(Distribution runtimeMs, Distribution memoryBytes) {
    }
}
