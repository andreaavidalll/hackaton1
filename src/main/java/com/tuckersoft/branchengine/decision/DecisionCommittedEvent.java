package com.tuckersoft.branchengine.decision;

import java.time.Instant;

/**
 * Todo lo que el listener necesita, porque corre en otro hilo despues del commit y
 * ahi ya no hay usuario autenticado en el SecurityContext.
 */
public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String recipientDisplayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        String rawInput,
        Instant createdAt,
        boolean simulateMailFailure
) {
}
