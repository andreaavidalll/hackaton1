package com.tuckersoft.branchengine.node.dto;

import com.tuckersoft.branchengine.node.StoryNode;

import java.time.Instant;

public record NodeResponse(
        Long id,
        String nodeCode,
        String title,
        String sceneText,
        Integer branchCapacity,
        Integer currentBranches,
        String primaryBranchCode,
        String glitchBranchCode,
        Instant createdAt
) {
    public static NodeResponse from(StoryNode node) {
        return new NodeResponse(
                node.getId(), node.getNodeCode(), node.getTitle(), node.getSceneText(),
                node.getBranchCapacity(), node.getCurrentBranches(),
                node.getPrimaryBranchCode(), node.getGlitchBranchCode(), node.getCreatedAt());
    }
}
