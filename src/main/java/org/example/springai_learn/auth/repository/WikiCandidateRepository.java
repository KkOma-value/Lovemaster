package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.WikiCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WikiCandidateRepository extends JpaRepository<WikiCandidate, String> {

    Page<WikiCandidate> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT c FROM WikiCandidate c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<WikiCandidate> findAllByStatus(@Param("status") String status);

    @Query("SELECT c.abstractSummary FROM WikiCandidate c " +
            "WHERE c.stage = :stage AND c.intent = :intent AND c.problem = :problem " +
            "AND c.status IN ('pending_review', 'approved') " +
            "AND c.abstractSummary IS NOT NULL")
    List<String> findSummariesByTopic(
            @Param("stage") String stage,
            @Param("intent") String intent,
            @Param("problem") String problem
    );
}
