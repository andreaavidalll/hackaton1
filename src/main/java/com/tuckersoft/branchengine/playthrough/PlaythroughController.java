package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.playthrough.dto.PathResponse;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughResponse;
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

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody PlaythroughRequest request) {
        Playthrough playthrough = playthroughService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaythroughResponse.from(playthrough));
    }

    @GetMapping
    public List<PlaythroughResponse> list() {
        return playthroughService.listForCurrentUser().stream().map(PlaythroughResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PlaythroughResponse get(@PathVariable Long id) {
        return PlaythroughResponse.from(playthroughService.getVisible(id));
    }

    @GetMapping("/{id}/path")
    public PathResponse path(@PathVariable Long id) {
        return playthroughService.buildPath(id);
    }
}
