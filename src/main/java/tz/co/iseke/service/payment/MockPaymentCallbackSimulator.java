package tz.co.iseke.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MockPaymentCallbackSimulator {

    private final PaymentOrchestrationService paymentOrchestrationService;
    private final ObjectMapper objectMapper;

    public MockPaymentCallbackSimulator(@Lazy PaymentOrchestrationService paymentOrchestrationService,
                                         ObjectMapper objectMapper) {
        this.paymentOrchestrationService = paymentOrchestrationService;
        this.objectMapper = objectMapper;
    }

    public void simulateSuccessCallback(PaymentRequest request, PaymentProvider provider) {
        // Capture values before async execution to avoid lazy-loading issues
        String requestNumber = request.getRequestNumber();
        String providerConversationId = request.getProviderConversationId();
        String providerReference = request.getProviderReference();
        var amount = request.getAmount();

        log.info("MOCK: Scheduling simulated callback for {} request {} in 5 seconds",
                provider, requestNumber);

        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            try {
                String payload = buildCallbackPayload(providerConversationId, providerReference,
                        requestNumber, amount, provider);
                log.info("MOCK: Firing simulated {} callback for request {}", provider, requestNumber);
                paymentOrchestrationService.handleCallback(provider, payload);
                log.info("MOCK: Simulated callback completed for request {}", requestNumber);
            } catch (Exception e) {
                log.error("MOCK: Failed to simulate callback for request {}: {}",
                        requestNumber, e.getMessage(), e);
            }
        });
    }

    String buildCallbackPayload(String providerConversationId, String providerReference,
                                String requestNumber, java.math.BigDecimal amount,
                                PaymentProvider provider) {
        try {
            return switch (provider) {
                case MPESA -> objectMapper.writeValueAsString(Map.of(
                        "output_ConversationID", providerConversationId != null
                                ? providerConversationId : "MOCK-CONV",
                        "output_ResponseCode", "INS-0",
                        "output_ResponseDesc", "Mock: Request processed successfully",
                        "output_TransactionID", "MOCK-TXN-" + System.currentTimeMillis(),
                        "output_ResultCode", "0"
                ));
                case TIGOPESA -> objectMapper.writeValueAsString(Map.of(
                        "transaction_ref_id", providerReference != null
                                ? providerReference : requestNumber,
                        "trans_status", "success",
                        "external_ref_id", "TIGO-EXT-" + System.currentTimeMillis(),
                        "amount", amount.toPlainString()
                ));
                case NMB_BANK -> objectMapper.writeValueAsString(Map.of(
                        "transactionReference", providerReference != null
                                ? providerReference : "NMB-REF",
                        "status", "SUCCESS",
                        "transactionId", "NMB-TXN-" + System.currentTimeMillis()
                ));
                default -> objectMapper.writeValueAsString(Map.of(
                        "status", "SUCCESS",
                        "reference", requestNumber
                ));
            };
        } catch (Exception e) {
            log.error("Failed to build mock callback payload: {}", e.getMessage());
            return "{}";
        }
    }
}
