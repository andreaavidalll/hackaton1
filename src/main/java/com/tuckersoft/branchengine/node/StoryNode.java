package com.tuckersoft.branchengine.node;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Una escena de Bandersnatch. primaryBranchCode/glitchBranchCode son Strings, no FK. */
@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nodeCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    private Integer currentBranches = 0;

    private String primaryBranchCode;

    private String glitchBranchCode;

    @Column(nullable = false)
    private Instant createdAt;
}
