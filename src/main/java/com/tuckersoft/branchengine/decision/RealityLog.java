package com.tuckersoft.branchengine.decision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Audita cada intento de envio del Informe de Realidad. */
@Entity
@Table(name = "reality_logs")
@Getter
@Setter
@NoArgsConstructor
public class RealityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    /** SENT o FAILED. */
    @Column(nullable = false)
    private String logStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant sentAt;

    @Column(nullable = false)
    private Instant createdAt;
}
