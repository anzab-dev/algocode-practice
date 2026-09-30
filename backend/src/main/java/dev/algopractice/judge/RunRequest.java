package dev.algopractice.judge;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * @param customInputs extra argument lists to run; expected answers come from the reference solution
 */
public record RunRequest(@NotNull @Size(max = 65536) String code, @Size(max = 10) List<JsonNode> customInputs) {
}
