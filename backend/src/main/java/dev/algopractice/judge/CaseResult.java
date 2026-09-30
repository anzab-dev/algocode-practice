package dev.algopractice.judge;

/**
 * One test case as shown to the user. JSON values are sent as (possibly truncated) text so
 * huge hidden inputs do not bloat responses.
 */
public record CaseResult(
        int index,
        boolean custom,
        String input,
        String expected,
        String output,
        boolean passed,
        String stdout,
        Double timeMs,
        String error) {
}
