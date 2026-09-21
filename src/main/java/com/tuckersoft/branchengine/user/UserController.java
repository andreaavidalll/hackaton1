package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.user.dto.RoleUpdateRequest;
import com.tuckersoft.branchengine.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/v1/users y PATCH /api/v1/users/{id}/role quedan restringidos a ROLE_ADMIN
 * en SecurityConfig (no aqui): si alguien los abre sin ese rol, Spring Security corta
 * antes de llegar a este controller.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(userService.getCurrentUser());
    }

    @GetMapping
    public List<UserResponse> listAll() {
        return userService.listAll().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        User actingAdmin = userService.getCurrentUser();
        User updated = userService.updateRole(id, request.role(), actingAdmin);
        return UserResponse.from(updated);
    }
}
