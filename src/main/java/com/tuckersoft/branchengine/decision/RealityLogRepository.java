package com.tuckersoft.branchengine.decision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {

    List<RealityLog> findByDecisionIdOrderByCreatedAtAsc(Long decisionId);
}
