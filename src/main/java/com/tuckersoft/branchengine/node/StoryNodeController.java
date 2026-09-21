package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.node.dto.NodeRequest;
import com.tuckersoft.branchengine.node.dto.NodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** POST queda restringido a ROLE_ADMIN en SecurityConfig (Parte 1), no aqui. */
@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    @PostMapping
    public ResponseEntity<NodeResponse> create(@Valid @RequestBody NodeRequest request) {
        StoryNode node = storyNodeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(NodeResponse.from(node));
    }

    @GetMapping
    public List<NodeResponse> list() {
        return storyNodeService.listAll().stream().map(NodeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NodeResponse get(@PathVariable Long id) {
        return NodeResponse.from(storyNodeService.getById(id));
    }
}
