package com.tuckersoft.branchengine.playthrough.dto;

import java.util.List;

public record PathResponse(
        Long playthroughId,
        String playerTag,
        String status,
        String endingCode,
        String startNodeCode,
        String currentNodeCode,
        List<PathStep> steps
) {
}
