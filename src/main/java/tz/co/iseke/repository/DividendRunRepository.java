package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.DividendRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DividendRunRepository extends JpaRepository<DividendRun, UUID> {
    Optional<DividendRun> findByYear(Integer year);
    List<DividendRun> findByStatus(String status);
}
