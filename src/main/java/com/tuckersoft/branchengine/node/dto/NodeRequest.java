package com.tuckersoft.branchengine.node.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NodeRequest(
        @NotBlank @Size(min = 3, max = 40) String nodeCode,
        @NotBlank @Size(min = 3, max = 80) String title,
        @NotBlank @Size(min = 10) String sceneText,
        @NotNull @Min(1) Integer branchCapacity,
        String primaryBranchCode,
        String glitchBranchCode
) {
}
