package com.tuckersoft.branchengine.decision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Bean SEPARADO del DecisionService: el service nunca conoce JavaMailSender ni a esta
 * clase. Corre en otro hilo, despues del commit de la transaccion que guardo la
 * decision, y necesita su PROPIA transaccion (REQUIRES_NEW) para que sus cambios se
 * persistan de verdad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Value("${app.admin.email}")
    private String remitente;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.decisionId()).orElse(null);
        if (decision == null) {
            return;
        }

        decision.setStatus("PROCESANDO");
        decisionRepository.save(decision);

        String subject = "[TUCKERSOFT] " + evento.branchType() + " en " + evento.playerTag()
                + " | Impacto " + evento.impactLevel();

        RealityLog realityLog = new RealityLog();
        realityLog.setDecision(decision);
        realityLog.setRecipientEmail(evento.recipientEmail());
        realityLog.setSubject(subject);
        realityLog.setCreatedAt(Instant.now());

        try {
            if (evento.simulateMailFailure()) {
                throw new IllegalStateException(
                        "Fallo simulado via X-Bandersnatch-Simulate: MAIL_FAILURE");
            }
            enviarCorreo(evento, subject);

            realityLog.setLogStatus("SENT");
            realityLog.setSentAt(Instant.now());
            decision.setStatus("ESTABILIZADA");
        } catch (MailException | IllegalStateException fallo) {
            realityLog.setLogStatus("FAILED");
            realityLog.setErrorMessage(fallo.getMessage());
            decision.setStatus("ERROR");
            log.error("Fallo al enviar el Informe de Realidad de la decision {}: {}",
                    decision.getId(), fallo.getMessage());
        }

        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);
        realityLogRepository.save(realityLog);

        System.out.println("[BRANCH-LOG] Decision ID: " + decision.getId()
                + " | Player: " + evento.playerTag()
                + " | Branch: " + evento.branchType()
                + " | Impact: " + evento.impactLevel()
                + " | Unit: " + evento.handlerUnit()
                + " | Node: " + evento.sourceNodeCode() + " -> " + evento.resolvedNodeCode()
                + " | Thread: " + Thread.currentThread().getName()
                + " | Status: " + decision.getStatus());
    }

    private void enviarCorreo(DecisionCommittedEvent evento, String subject) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(evento.recipientEmail());
        mensaje.setSubject(subject);
        mensaje.setText(cuerpo(evento));
        mailSender.send(mensaje);
    }

    private String cuerpo(DecisionCommittedEvent evento) {
        String finalCode = evento.endingCode() == null ? "-" : evento.endingCode();
        String separador = "━".repeat(36);
        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                %s
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                %s

                Decision original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                evento.recipientDisplayName(),
                separador,
                evento.decisionId(),
                evento.playerTag(),
                evento.branchType(),
                evento.impactLevel(),
                evento.handlerUnit(),
                evento.outcomeCode(),
                evento.sourceNodeCode(),
                evento.resolvedNodeCode(),
                evento.playthroughStatus(),
                evento.lucidity(),
                evento.controlLevel(),
                finalCode,
                evento.createdAt(),
                separador,
                evento.rawInput());
    }
}
