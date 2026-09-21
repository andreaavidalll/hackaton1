package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.NotFoundException;
import com.tuckersoft.branchengine.node.dto.NodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    public StoryNode create(NodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.nodeCode())) {
            throw new ConflictException("Ya existe un nodo con el codigo " + request.nodeCode());
        }
        StoryNode node = new StoryNode();
        node.setNodeCode(request.nodeCode());
        node.setTitle(request.title());
        node.setSceneText(request.sceneText());
        node.setBranchCapacity(request.branchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.primaryBranchCode());
        node.setGlitchBranchCode(request.glitchBranchCode());
        node.setCreatedAt(Instant.now());
        return storyNodeRepository.save(node);
    }

    public StoryNode getById(Long id) {
        return storyNodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nodo no encontrado: " + id));
    }

    public StoryNode getByCode(String nodeCode) {
        return findByCode(nodeCode)
                .orElseThrow(() -> new NotFoundException("Nodo no encontrado: " + nodeCode));
    }

    /** Usado por la Parte 3 para resolver el nodo destino sin lanzar 404 si no existe. */
    public Optional<StoryNode> findByCode(String nodeCode) {
        return storyNodeRepository.findByNodeCode(nodeCode);
    }

    public List<StoryNode> listAll() {
        return storyNodeRepository.findAll();
    }

    public StoryNode save(StoryNode node) {
        return storyNodeRepository.save(node);
    }
}
