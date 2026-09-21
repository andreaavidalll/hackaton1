package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.decision.dto.DecisionRequest;
import com.tuckersoft.branchengine.decision.dto.DecisionResponse;
import com.tuckersoft.branchengine.decision.dto.PageResponse;
import com.tuckersoft.branchengine.decision.dto.RealityLogResponse;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;
    private final RealityLogRepository realityLogRepository;

    @PostMapping
    public ResponseEntity<DecisionResponse> create(
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {
        Decision decision = decisionService.decide(request, simulate);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(decision));
    }

    @GetMapping
    public PageResponse<DecisionResponse> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DecisionResponse> pagina = decisionService
                .search(branchType, impactLevel, status, playthroughId, page, size)
                .map(this::toResponse);
        return PageResponse.from(pagina);
    }

    @GetMapping("/{id}")
    public DecisionResponse get(@PathVariable Long id) {
        return toResponse(decisionService.getVisible(id));
    }

    @GetMapping("/{id}/reality-logs")
    public List<RealityLogResponse> realityLogs(@PathVariable Long id) {
        Decision decision = decisionService.getVisible(id);
        return realityLogRepository.findByDecisionIdOrderByCreatedAtAsc(decision.getId())
                .stream().map(RealityLogResponse::from).toList();
    }

    private DecisionResponse toResponse(Decision decision) {
        Playthrough playthrough = decision.getPlaythrough();
        return new DecisionResponse(
                decision.getId(),
                playthrough.getId(),
                playthrough.getPlayerTag(),
                decision.getNode().getNodeCode(),
                decision.getResolvedNodeCode(),
                decision.getRawInput(),
                decision.getBranchType(),
                decision.getImpactLevel(),
                decision.getHandlerUnit(),
                decision.getOutcomeCode(),
                decision.getStatus(),
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                decision.getCreatedAt(),
                decision.getUpdatedAt());
    }
}
