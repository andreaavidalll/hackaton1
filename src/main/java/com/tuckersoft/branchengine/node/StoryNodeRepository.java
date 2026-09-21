package com.tuckersoft.branchengine.node;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryNodeRepository extends JpaRepository<StoryNode, Long> {

    Optional<StoryNode> findByNodeCode(String nodeCode);

    boolean existsByNodeCode(String nodeCode);
}
