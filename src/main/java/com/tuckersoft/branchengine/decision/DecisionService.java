package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.common.exception.BadRequestException;
import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.ForbiddenException;
import com.tuckersoft.branchengine.common.exception.NotFoundException;
import com.tuckersoft.branchengine.decision.dto.DecisionRequest;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeService;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final Set<String> IMPACTOS_VALIDOS = Set.of("LEVE", "MODERADO", "GRAVE", "CRITICO");

    private static final Map<String, Integer> DELTA_LUCIDEZ = Map.of(
            "LEVE", -5, "MODERADO", -15, "GRAVE", -30, "CRITICO", -40);
    private static final Map<String, Integer> DELTA_CONTROL = Map.of(
            "LEVE", 5, "MODERADO", 10, "GRAVE", 20, "CRITICO", 45);

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeService storyNodeService;
    private final UserService userService;
    private final DecisionClassifier decisionClassifier;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Decision decide(DecisionRequest request, String simulateHeader) {
        User current = userService.getCurrentUser();

        Playthrough playthrough = playthroughRepository.findById(request.playthroughId())
                .orElseThrow(() -> new NotFoundException("Partida no encontrada: " + request.playthroughId()));

        if (!playthrough.getUser().getId().equals(current.getId())) {
            throw new ForbiddenException("No puedes decidir sobre una partida que no es tuya");
        }
        if (!"ACTIVA".equals(playthrough.getStatus())) {
            throw new ConflictException("La partida " + playthrough.getId() + " ya esta FINALIZADA");
        }
        if (!IMPACTOS_VALIDOS.contains(request.impactLevel())) {
            throw new BadRequestException("impactLevel debe ser LEVE, MODERADO, GRAVE o CRITICO");
        }

        DecisionClassifier.Resultado clasificacion = decisionClassifier.clasificar(request.rawInput());
        StoryNode origen = playthrough.getCurrentNode();

        Decision decision = new Decision();
        decision.setPlaythrough(playthrough);
        decision.setNode(origen);
        decision.setRawInput(request.rawInput());
        decision.setBranchType(clasificacion.branchType());
        decision.setImpactLevel(request.impactLevel());
        decision.setHandlerUnit(clasificacion.handlerUnit());
        decision.setOutcomeCode(clasificacion.outcomeCode());
        Instant ahora = Instant.now();
        decision.setCreatedAt(ahora);
        decision.setUpdatedAt(ahora);

        if ("ENTRADA_CORRUPTA".equals(clasificacion.branchType())) {
            decision.setResolvedNodeCode(null);
            decision.setStatus("ERROR");
            return decisionRepository.save(decision);
        }

        aplicarStatsYResolverNodo(decision, playthrough, origen);
        decision.setStatus("REGISTRADA");

        playthrough.setUpdatedAt(Instant.now());
        playthroughRepository.save(playthrough);
        Decision guardada = decisionRepository.save(decision);

        eventPublisher.publishEvent(new DecisionCommittedEvent(
                guardada.getId(),
                current.getEmail(),
                current.getDisplayName(),
                playthrough.getPlayerTag(),
                guardada.getBranchType(),
                guardada.getImpactLevel(),
                guardada.getHandlerUnit(),
                guardada.getOutcomeCode(),
                origen.getNodeCode(),
                guardada.getResolvedNodeCode(),
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                guardada.getRawInput(),
                guardada.getCreatedAt(),
                "MAIL_FAILURE".equals(simulateHeader)));

        return guardada;
    }

    private void aplicarStatsYResolverNodo(Decision decision, Playthrough playthrough, StoryNode origen) {
        int deltaLucidez = DELTA_LUCIDEZ.get(decision.getImpactLevel());
        int deltaControl = DELTA_CONTROL.get(decision.getImpactLevel());

        int nuevaLucidez = Math.max(0, Math.min(100, playthrough.getLucidity() + deltaLucidez));
        int nuevoControl = Math.max(0, Math.min(100, playthrough.getControlLevel() + deltaControl));
        playthrough.setLucidity(nuevaLucidez);
        playthrough.setControlLevel(nuevoControl);

        boolean porGlitch = "RUPTURA_CUARTA_PARED".equals(decision.getBranchType())
                || "CRITICO".equals(decision.getImpactLevel());
        String destino = porGlitch ? origen.getGlitchBranchCode() : origen.getPrimaryBranchCode();
        decision.setResolvedNodeCode(destino);

        if (nuevoControl >= 100) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_PAC_SYMBOL");
            return;
        }
        if (nuevaLucidez <= 0) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_WHITE_BEAR");
            return;
        }

        Optional<StoryNode> nodoDestino = destino == null ? Optional.empty() : storyNodeService.findByCode(destino);
        if (nodoDestino.isEmpty()) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_NETFLIX_CUT");
        } else {
            playthrough.setStatus("ACTIVA");
            playthrough.setCurrentNode(nodoDestino.get());
        }
    }

    public Decision getById(Long id) {
        return decisionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Decision no encontrada: " + id));
    }

    public Decision getVisible(Long id) {
        Decision decision = getById(id);
        User current = userService.getCurrentUser();
        if (!ROLE_ADMIN.equals(current.getRole()) && !decision.getPlaythrough().getUser().getId().equals(current.getId())) {
            throw new ForbiddenException("No puedes ver una decision que no es tuya");
        }
        return decision;
    }

    public Page<Decision> search(String branchType, String impactLevel, String status, Long playthroughId, int page, int size) {
        User current = userService.getCurrentUser();

        Specification<Decision> spec = Specification.where(null);
        if (!ROLE_ADMIN.equals(current.getRole())) {
            Long userId = current.getId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("user").get("id"), userId));
        }
        if (branchType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchType"), branchType));
        }
        if (impactLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("impactLevel"), impactLevel));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (playthroughId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("id"), playthroughId));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return decisionRepository.findAll(spec, pageable);
    }
}
