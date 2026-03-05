package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.PaymentReconciliation;
import tz.co.iseke.enums.PaymentProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliation, UUID> {
    Optional<PaymentReconciliation> findByProviderAndReconciliationDate(PaymentProvider provider, LocalDate date);
    List<PaymentReconciliation> findByReconciliationDate(LocalDate date);
    List<PaymentReconciliation> findByProvider(PaymentProvider provider);
}
