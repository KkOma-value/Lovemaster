package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.WikiStrategyScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WikiStrategyScoreRepository extends JpaRepository<WikiStrategyScore, String> {

    Optional<WikiStrategyScore> findByTopicKeyAndStrategyId(String topicKey, String strategyId);

    List<WikiStrategyScore> findByTopicKeyOrderByRankScoreDesc(String topicKey, Pageable pageable);

    List<WikiStrategyScore> findAllByOrderByRankScoreDesc(Pageable pageable);
}
