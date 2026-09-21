package com.tuckersoft.branchengine.decision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull Long playthroughId,
        @NotBlank @Size(min = 10) String rawInput,
        @NotBlank String impactLevel
) {
}
