package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.common.exception.BadRequestException;
import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.ForbiddenException;
import com.tuckersoft.branchengine.common.exception.NotFoundException;
import com.tuckersoft.branchengine.decision.Decision;
import com.tuckersoft.branchengine.decision.DecisionRepository;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeService;
import com.tuckersoft.branchengine.playthrough.dto.PathResponse;
import com.tuckersoft.branchengine.playthrough.dto.PathStep;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeService storyNodeService;
    private final UserService userService;
    private final DecisionRepository decisionRepository;

    public Playthrough create(PlaythroughRequest request) {
        User owner = userService.getCurrentUser();
        StoryNode startNode = storyNodeService.getByCode(request.startNodeCode());

        if (playthroughRepository.existsByPlayerTag(request.playerTag())) {
            throw new ConflictException("Ya existe una partida con el playerTag " + request.playerTag());
        }
        if (startNode.getCurrentBranches() >= startNode.getBranchCapacity()) {
            throw new BadRequestException("El nodo " + startNode.getNodeCode() + " esta lleno");
        }

        Playthrough playthrough = new Playthrough();
        playthrough.setPlayerTag(request.playerTag());
        playthrough.setUser(owner);
        playthrough.setStartNodeCode(startNode.getNodeCode());
        playthrough.setCurrentNode(startNode);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");
        playthrough.setEndingCode(null);
        Instant now = Instant.now();
        playthrough.setCreatedAt(now);
        playthrough.setUpdatedAt(now);

        startNode.setCurrentBranches(startNode.getCurrentBranches() + 1);
        storyNodeService.save(startNode);

        return playthroughRepository.save(playthrough);
    }

    public Playthrough getById(Long id) {
        return playthroughRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Partida no encontrada: " + id));
    }

    public List<Playthrough> listForCurrentUser() {
        User current = userService.getCurrentUser();
        if (ROLE_ADMIN.equals(current.getRole())) {
            return playthroughRepository.findAllByOrderByCreatedAtDesc();
        }
        return playthroughRepository.findByUserIdOrderByCreatedAtDesc(current.getId());
    }

    /** El dueno o un ADMIN pueden LEER cualquier partida; el resto, 403. */
    public Playthrough getVisible(Long id) {
        Playthrough playthrough = getById(id);
        User current = userService.getCurrentUser();
        if (!ROLE_ADMIN.equals(current.getRole()) && !playthrough.getUser().getId().equals(current.getId())) {
            throw new ForbiddenException("No puedes ver una partida que no es tuya");
        }
        return playthrough;
    }

    public PathResponse buildPath(Long id) {
        Playthrough playthrough = getVisible(id);
        List<Decision> decisiones = decisionRepository
                .findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(playthrough.getId());

        List<PathStep> steps = new ArrayList<>();
        int orden = 1;
        for (Decision decision : decisiones) {
            steps.add(new PathStep(
                    orden++,
                    decision.getId(),
                    decision.getNode().getNodeCode(),
                    decision.getResolvedNodeCode(),
                    decision.getBranchType(),
                    decision.getImpactLevel(),
                    decision.getCreatedAt()));
        }

        return new PathResponse(
                playthrough.getId(),
                playthrough.getPlayerTag(),
                playthrough.getStatus(),
                playthrough.getEndingCode(),
                playthrough.getStartNodeCode(),
                playthrough.getCurrentNode().getNodeCode(),
                steps);
    }

    public Playthrough save(Playthrough playthrough) {
        return playthroughRepository.save(playthrough);
    }
}
