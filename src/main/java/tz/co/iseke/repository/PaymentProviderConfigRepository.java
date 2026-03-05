package tz.co.iseke.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.co.iseke.entity.PaymentProviderConfig;
import tz.co.iseke.enums.PaymentProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProviderConfigRepository extends JpaRepository<PaymentProviderConfig, UUID> {
    Optional<PaymentProviderConfig> findByProvider(PaymentProvider provider);
    List<PaymentProviderConfig> findByIsEnabledTrue();
}
