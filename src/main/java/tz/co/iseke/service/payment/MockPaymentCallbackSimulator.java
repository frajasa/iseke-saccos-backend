package tz.co.iseke.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;

import java.util.Map;

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

    @Async
    public void simulateSuccessCallback(PaymentRequest request, PaymentProvider provider) {
        try {
            log.info("MOCK: Scheduling simulated callback for {} request {} in 3 seconds",
                    provider, request.getRequestNumber());
            Thread.sleep(3000);

            String payload = buildCallbackPayload(request, provider);
            log.info("MOCK: Firing simulated {} callback for request {}", provider, request.getRequestNumber());
            paymentOrchestrationService.handleCallback(provider, payload);
            log.info("MOCK: Simulated callback completed for request {}", request.getRequestNumber());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MOCK: Callback simulation interrupted for request {}", request.getRequestNumber());
        } catch (Exception e) {
            log.error("MOCK: Failed to simulate callback for request {}: {}",
                    request.getRequestNumber(), e.getMessage());
        }
    }

    String buildCallbackPayload(PaymentRequest request, PaymentProvider provider) {
        try {
            return switch (provider) {
                case MPESA -> objectMapper.writeValueAsString(Map.of(
                        "output_ConversationID", request.getProviderConversationId() != null
                                ? request.getProviderConversationId() : "MOCK-CONV",
                        "output_ResponseCode", "INS-0",
                        "output_ResponseDesc", "Mock: Request processed successfully",
                        "output_TransactionID", "MOCK-TXN-" + System.currentTimeMillis(),
                        "output_ResultCode", "0"
                ));
                case TIGOPESA -> objectMapper.writeValueAsString(Map.of(
                        "transaction_ref_id", request.getProviderReference() != null
                                ? request.getProviderReference() : request.getRequestNumber(),
                        "trans_status", "success",
                        "external_ref_id", "TIGO-EXT-" + System.currentTimeMillis(),
                        "amount", request.getAmount().toPlainString()
                ));
                case NMB_BANK -> objectMapper.writeValueAsString(Map.of(
                        "transactionReference", request.getProviderReference() != null
                                ? request.getProviderReference() : "NMB-REF",
                        "status", "SUCCESS",
                        "transactionId", "NMB-TXN-" + System.currentTimeMillis()
                ));
                default -> objectMapper.writeValueAsString(Map.of(
                        "status", "SUCCESS",
                        "reference", request.getRequestNumber()
                ));
            };
        } catch (Exception e) {
            log.error("Failed to build mock callback payload: {}", e.getMessage());
            return "{}";
        }
    }
}
