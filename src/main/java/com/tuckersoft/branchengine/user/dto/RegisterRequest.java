package com.tuckersoft.branchengine.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Un "role" en el body se ignora siempre: el rol se asigna en el service, nunca desde el request. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(min = 3, max = 60) String displayName
) {
}
