package com.tuckersoft.branchengine.user.dto;

public record AuthResponse(String token, String type, String email, String displayName, String role) {
}
