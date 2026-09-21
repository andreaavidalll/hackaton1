package com.tuckersoft.branchengine.decision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {

    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId);
}
