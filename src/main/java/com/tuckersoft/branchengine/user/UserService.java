package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.common.exception.BadRequestException;
import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.NotFoundException;
import com.tuckersoft.branchengine.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe una cuenta con el email " + request.email());
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(ROLE_USER);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + email));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    /**
     * Punto unico para saber quien esta autenticado. Lo usan tambien los modulos de
     * Nodos/Partidas y Decisiones para resolver el dueno de cada recurso.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No hay un usuario autenticado");
        }
        return getByEmail(authentication.getName());
    }

    public User updateRole(Long targetId, String newRole, User actingAdmin) {
        if (!ROLE_USER.equals(newRole) && !ROLE_ADMIN.equals(newRole)) {
            throw new BadRequestException("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }
        User target = getById(targetId);
        if (target.getId().equals(actingAdmin.getId())) {
            throw new BadRequestException("Un administrador no puede cambiar su propio rol");
        }
        target.setRole(newRole);
        return userRepository.save(target);
    }
}
