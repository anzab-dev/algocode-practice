package dev.algopractice.judge;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRequest(@NotNull @Size(max = 65536) String code) {
}
