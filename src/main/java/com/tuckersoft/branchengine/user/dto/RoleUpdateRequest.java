package com.tuckersoft.branchengine.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(@NotBlank String role) {
}
