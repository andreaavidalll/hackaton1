package com.tuckersoft.branchengine.playthrough;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {

    Optional<Playthrough> findByPlayerTag(String playerTag);

    boolean existsByPlayerTag(String playerTag);

    List<Playthrough> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Playthrough> findAllByOrderByCreatedAtDesc();
}
