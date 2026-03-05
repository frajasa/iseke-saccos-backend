package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.CreditScore;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {
    Optional<CreditScore> findTopByMemberIdOrderByCalculatedAtDesc(UUID memberId);
}
