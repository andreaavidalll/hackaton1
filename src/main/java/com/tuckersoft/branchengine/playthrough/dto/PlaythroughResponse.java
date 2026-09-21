package com.tuckersoft.branchengine.playthrough.dto;

import com.tuckersoft.branchengine.playthrough.Playthrough;

import java.time.Instant;

public record PlaythroughResponse(
        Long id,
        String playerTag,
        String ownerEmail,
        String startNodeCode,
        String currentNodeCode,
        Integer lucidity,
        Integer controlLevel,
        String status,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlaythroughResponse from(Playthrough playthrough) {
        return new PlaythroughResponse(
                playthrough.getId(),
                playthrough.getPlayerTag(),
                playthrough.getUser().getEmail(),
                playthrough.getStartNodeCode(),
                playthrough.getCurrentNode().getNodeCode(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getStatus(),
                playthrough.getEndingCode(),
                playthrough.getCreatedAt(),
                playthrough.getUpdatedAt());
    }
}
