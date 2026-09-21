package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "playthroughs")
@Getter
@Setter
@NoArgsConstructor
public class Playthrough {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String playerTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** No cambia nunca: es el nodo con el que abrio la partida. */
    @Column(nullable = false)
    private String startNodeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_node_id", nullable = false)
    private StoryNode currentNode;

    @Column(nullable = false)
    private Integer lucidity = 100;

    @Column(nullable = false)
    private Integer controlLevel = 0;

    @Column(nullable = false)
    private String status = "ACTIVA";

    private String endingCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
