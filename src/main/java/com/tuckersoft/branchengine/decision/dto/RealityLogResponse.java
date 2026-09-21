package com.tuckersoft.branchengine.decision.dto;

import com.tuckersoft.branchengine.decision.RealityLog;

import java.time.Instant;

public record RealityLogResponse(
        Long id,
        Long decisionId,
        String recipientEmail,
        String subject,
        String logStatus,
        String errorMessage,
        Instant sentAt,
        Instant createdAt
) {
    public static RealityLogResponse from(RealityLog log) {
        return new RealityLogResponse(
                log.getId(), log.getDecision().getId(), log.getRecipientEmail(), log.getSubject(),
                log.getLogStatus(), log.getErrorMessage(), log.getSentAt(), log.getCreatedAt());
    }
}
