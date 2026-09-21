package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.decision.dto.DecisionRequest;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeService;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los 5 tests que pide el enunciado. Todo mockeado (Mockito puro, sin @SpringBootTest):
 * corren sin PostgreSQL ni red.
 */
class DecisionServiceTest {

    private DecisionRepository decisionRepository;
    private PlaythroughRepository playthroughRepository;
    private StoryNodeService storyNodeService;
    private UserService userService;
    private ApplicationEventPublisher eventPublisher;
    private DecisionService decisionService;

    private User user;
    private StoryNode origen;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        decisionRepository = mock(DecisionRepository.class);
        playthroughRepository = mock(PlaythroughRepository.class);
        storyNodeService = mock(StoryNodeService.class);
        userService = mock(UserService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        decisionService = new DecisionService(
                decisionRepository, playthroughRepository, storyNodeService,
                userService, new DecisionClassifier(), eventPublisher);

        user = new User();
        user.setId(1L);
        user.setEmail("qa@tuckersoft.test");
        user.setDisplayName("Ada Lovelace");
        user.setRole("ROLE_USER");

        origen = new StoryNode();
        origen.setId(10L);
        origen.setNodeCode("NODE-ORIGEN");
        origen.setPrimaryBranchCode("NODE-BUS");
        origen.setGlitchBranchCode("NODE-ESPEJO");
        origen.setBranchCapacity(50);
        origen.setCurrentBranches(0);

        playthrough = new Playthrough();
        playthrough.setId(100L);
        playthrough.setUser(user);
        playthrough.setPlayerTag("STEFAN-TEST");
        playthrough.setStartNodeCode("NODE-ORIGEN");
        playthrough.setCurrentNode(origen);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");

        when(userService.getCurrentUser()).thenReturn(user);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playthroughRepository.save(any(Playthrough.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private StoryNode nodo(String code) {
        StoryNode n = new StoryNode();
        n.setNodeCode(code);
        return n;
    }

    @Test
    void texto_con_camara_y_destruye_se_clasifica_como_ruptura_cuarta_pared_por_precedencia() {
        when(storyNodeService.findByCode("NODE-ESPEJO")).thenReturn(Optional.of(nodo("NODE-ESPEJO")));

        DecisionRequest request = new DecisionRequest(
                100L, "Stefan destruye la camara que lo estaba grabando.", "LEVE");
        Decision decision = decisionService.decide(request, null);

        assertEquals("RUPTURA_CUARTA_PARED", decision.getBranchType());
    }

    @Test
    void texto_sin_letras_es_entrada_corrupta_y_no_toca_la_partida() {
        DecisionRequest request = new DecisionRequest(100L, "%%%% 01001 ### @@@ 110", "CRITICO");
        Decision decision = decisionService.decide(request, null);

        assertEquals("ENTRADA_CORRUPTA", decision.getBranchType());
        assertEquals("ERROR", decision.getStatus());
        assertNull(decision.getResolvedNodeCode());
        assertEquals(100, playthrough.getLucidity());
        assertEquals(0, playthrough.getControlLevel());
        assertEquals("ACTIVA", playthrough.getStatus());
    }

    @Test
    void impacto_critico_ajusta_los_stats_y_respeta_los_limites() {
        when(storyNodeService.findByCode("NODE-ESPEJO")).thenReturn(Optional.of(nodo("NODE-ESPEJO")));

        DecisionRequest primera = new DecisionRequest(
                100L, "Stefan sigue el guion sin cuestionar nada.", "CRITICO");
        decisionService.decide(primera, null);
        assertEquals(60, playthrough.getLucidity());
        assertEquals(45, playthrough.getControlLevel());

        playthrough.setLucidity(10);
        playthrough.setControlLevel(90);
        DecisionRequest segunda = new DecisionRequest(
                100L, "Stefan sigue el guion sin cuestionar nada, otra vez.", "CRITICO");
        decisionService.decide(segunda, null);
        assertEquals(0, playthrough.getLucidity());
        assertEquals(100, playthrough.getControlLevel());
    }

    @Test
    void controlLevel_al_maximo_gana_sobre_lucidity_agotada() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(55);

        DecisionRequest request = new DecisionRequest(
                100L, "Stefan sigue el guion sin cuestionar nada.", "CRITICO");
        decisionService.decide(request, null);

        assertEquals(0, playthrough.getLucidity());
        assertEquals(100, playthrough.getControlLevel());
        assertEquals("FINALIZADA", playthrough.getStatus());
        assertEquals("ENDING_PAC_SYMBOL", playthrough.getEndingCode());
    }

    @Test
    void publica_el_evento_una_vez_en_decision_normal_y_ninguna_en_entrada_corrupta() {
        when(storyNodeService.findByCode("NODE-BUS")).thenReturn(Optional.of(nodo("NODE-BUS")));

        DecisionRequest normal = new DecisionRequest(
                100L, "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        decisionService.decide(normal, null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        DecisionRequest corrupta = new DecisionRequest(100L, "%%%% #### 1010", "LEVE");
        decisionService.decide(corrupta, null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }
}
