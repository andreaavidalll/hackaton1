package com.tuckersoft.branchengine.decision.dto;

import java.time.Instant;

/** playthroughStatus/lucidity/controlLevel/endingCode son el estado de la partida DESPUES de aplicar la decision. */
public record DecisionResponse(
        Long id,
        Long playthroughId,
        String playerTag,
        String sourceNodeCode,
        String resolvedNodeCode,
        String rawInput,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String status,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {
}
