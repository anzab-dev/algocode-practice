package dev.algopractice.execution;

import tools.jackson.databind.JsonNode;

/** One test executed by the harness. {@code output} is null when the method threw. */
public record TestRun(JsonNode output, String stdout, long timeNanos, RuntimeFailure error) {

    public record RuntimeFailure(String type, String message, String trace) {
    }
}
