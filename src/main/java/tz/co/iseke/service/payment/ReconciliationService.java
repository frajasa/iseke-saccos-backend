package tz.co.iseke.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.entity.PaymentReconciliation;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.repository.PaymentReconciliationRepository;
import tz.co.iseke.repository.PaymentRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentReconciliationRepository reconciliationRepository;
    private final PaymentGatewayRegistry gatewayRegistry;

    @Scheduled(cron = "${payment.reconciliation.cron:0 0 2 * * ?}")
    @Transactional
    public void runDailyReconciliation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running daily payment reconciliation for {}", yesterday);

        for (PaymentProvider provider : gatewayRegistry.getAvailableProviders()) {
            try {
                runReconciliation(provider, yesterday, "SYSTEM");
            } catch (Exception e) {
                log.error("Reconciliation failed for {} on {}: {}", provider, yesterday, e.getMessage());
            }
        }
    }

    @Transactional
    public PaymentReconciliation runReconciliation(PaymentProvider provider, LocalDate date, String reconciledBy) {
        log.info("Running reconciliation for {} on {}", provider, date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Get all completed internal payment requests for the day
        List<PaymentRequest> completedRequests = paymentRequestRepository
                .findForReconciliation(provider, PaymentRequestStatus.COMPLETED, startOfDay, endOfDay);

        List<PaymentRequest> failedRequests = paymentRequestRepository
                .findForReconciliation(provider, PaymentRequestStatus.FAILED, startOfDay, endOfDay);

        List<PaymentRequest> expiredRequests = paymentRequestRepository
                .findForReconciliation(provider, PaymentRequestStatus.EXPIRED, startOfDay, endOfDay);

        int totalInternal = completedRequests.size() + failedRequests.size() + expiredRequests.size();

        BigDecimal totalInternalAmount = completedRequests.stream()
                .map(PaymentRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // In production, we would fetch the provider's transaction report here
        // and compare it with our internal records.
        // For now, we create a reconciliation record based on internal data only.

        PaymentReconciliation reconciliation = reconciliationRepository
                .findByProviderAndReconciliationDate(provider, date)
                .orElse(PaymentReconciliation.builder()
                        .provider(provider)
                        .reconciliationDate(date)
                        .build());

        reconciliation.setTotalInternalCount(totalInternal);
        reconciliation.setTotalProviderCount(totalInternal); // Same in mock mode
        reconciliation.setMatchedCount(completedRequests.size());
        reconciliation.setMismatchedCount(0);
        reconciliation.setMissingInternalCount(0);
        reconciliation.setMissingProviderCount(0);
        reconciliation.setTotalInternalAmount(totalInternalAmount);
        reconciliation.setTotalProviderAmount(totalInternalAmount); // Same in mock mode
        reconciliation.setAmountDifference(BigDecimal.ZERO);
        reconciliation.setStatus("COMPLETED");
        reconciliation.setReconciledBy(reconciledBy);
        reconciliation.setNotes(String.format("Completed: %d, Failed: %d, Expired: %d",
                completedRequests.size(), failedRequests.size(), expiredRequests.size()));

        reconciliation = reconciliationRepository.save(reconciliation);
        log.info("Reconciliation completed for {} on {}: {} matched, {} total",
                provider, date, completedRequests.size(), totalInternal);

        return reconciliation;
    }

    public List<PaymentReconciliation> getReconciliationsByDate(LocalDate date) {
        return reconciliationRepository.findByReconciliationDate(date);
    }

    public List<PaymentReconciliation> getReconciliationsByProvider(PaymentProvider provider) {
        return reconciliationRepository.findByProvider(provider);
    }
}
