package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.security.JwtService;
import com.tuckersoft.branchengine.user.dto.AuthResponse;
import com.tuckersoft.branchengine.user.dto.LoginRequest;
import com.tuckersoft.branchengine.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // BadCredentialsException (contrasena mala O email inexistente, misma excepcion
        // por hideUserNotFoundExceptions=true) sube al GlobalExceptionHandler -> 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userService.getByEmail(request.email());
        return ResponseEntity.ok(toAuthResponse(user));
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
