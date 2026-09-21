package com.tuckersoft.branchengine.user.dto;

import com.tuckersoft.branchengine.user.User;

import java.time.Instant;

/** Nunca incluye 'password': es el DTO que sale por Controller, jamas la entidad JPA. */
public record UserResponse(Long id, String email, String displayName, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
