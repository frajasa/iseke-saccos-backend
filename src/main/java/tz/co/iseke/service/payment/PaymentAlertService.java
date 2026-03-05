package tz.co.iseke.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tz.co.iseke.entity.PaymentReconciliation;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAlertService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final NotificationService notificationService;

    private static final double FAILURE_RATE_THRESHOLD = 0.3; // 30%
    private static final int MINIMUM_SAMPLE_SIZE = 10;

    @Scheduled(fixedDelay = 600000) // Every 10 minutes
    public void checkFailureRates() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        for (PaymentProvider provider : PaymentProvider.values()) {
            if (provider == PaymentProvider.INTERNAL) continue;

            long completed = paymentRequestRepository.countByProviderAndStatusSince(provider, PaymentRequestStatus.COMPLETED, since);
            long failed = paymentRequestRepository.countByProviderAndStatusSince(provider, PaymentRequestStatus.FAILED, since);
            long total = completed + failed;

            if (total >= MINIMUM_SAMPLE_SIZE) {
                double failureRate = (double) failed / total;
                if (failureRate > FAILURE_RATE_THRESHOLD) {
                    String message = String.format(
                            "ALERT: %s payment failure rate is %.1f%% (%d failed out of %d) in the last hour",
                            provider, failureRate * 100, failed, total);
                    log.warn(message);
                    // notificationService can be used to send email/SMS alerts
                }
            }
        }
    }

    public void alertReconciliationDiscrepancy(PaymentReconciliation reconciliation) {
        if (reconciliation.getMismatchedCount() > 0
                || reconciliation.getAmountDifference().compareTo(BigDecimal.ZERO) != 0) {
            String message = String.format(
                    "Reconciliation discrepancy for %s on %s: %d mismatches, amount diff: %s TZS",
                    reconciliation.getProvider(),
                    reconciliation.getReconciliationDate(),
                    reconciliation.getMismatchedCount(),
                    reconciliation.getAmountDifference().toPlainString());
            log.warn(message);
        }
    }
}
