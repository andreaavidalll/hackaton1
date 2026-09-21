package com.tuckersoft.branchengine.security;

import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Crea al administrador al arrancar, leyendo ADMIN_NAME / ADMIN_EMAIL / ADMIN_PASSWORD
 * del .env. La contrasena se guarda codificada con BCrypt, nunca en texto plano.
 * Si ya existe un usuario con ese email, no hace nada (no lo vuelve a degradar/promover).
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.display-name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminName);
        admin.setRole("ROLE_ADMIN");
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
    }
}
