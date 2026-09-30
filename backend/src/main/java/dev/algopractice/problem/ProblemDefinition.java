package dev.algopractice.problem;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * A problem as authored on disk under {@code resources/problems/<slug>/}:
 * {@code problem.yaml} (metadata and starter code), {@code description.md},
 * {@code tests.json} and {@code Solution.java} (the reference solution).
 * {@code formerSlugs} lists the directory names a problem had before it was renamed.
 */
public record ProblemDefinition(
        String slug,
        Meta meta,
        String description,
        String referenceCode,
        List<Test> tests,
        String contentHash) {

    public record Meta(
            String title,
            Difficulty difficulty,
            List<String> tags,
            String method,
            List<String> formerSlugs,
            List<Param> params,
            String returnType,
            CompareMode compare,
            Integer timeLimitMs,
            Integer order,
            List<String> hints,
            String starterCode) {
    }

    public record Param(String name, String type) {
    }

    public record Test(JsonNode args, JsonNode expected, boolean sample) {
    }
}
