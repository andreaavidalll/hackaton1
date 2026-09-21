package com.tuckersoft.branchengine.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** El analista de QA: se registra, entra con su cuenta y es dueno de sus Playthrough. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Siempre codificada con BCrypt. Nunca se serializa en un DTO de respuesta. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    /** "ROLE_USER" o "ROLE_ADMIN". Se lee de la base de datos en cada peticion. */
    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(nullable = false)
    private Instant createdAt;
}
