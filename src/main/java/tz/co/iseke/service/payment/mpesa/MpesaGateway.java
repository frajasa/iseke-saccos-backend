package tz.co.iseke.service.payment.mpesa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import tz.co.iseke.config.MpesaProperties;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.service.payment.MockPaymentCallbackSimulator;
import tz.co.iseke.service.payment.PaymentGateway;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaGateway implements PaymentGateway {

    private final MpesaProperties mpesaProperties;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ObjectMapper objectMapper;
    private final MockPaymentCallbackSimulator callbackSimulator;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MPESA;
    }

    @Override
    public PaymentRequest initiateCollection(PaymentRequest request) {
        if (!mpesaProperties.isEnabled()) {
            return simulateCollection(request);
        }

        try {
            String bearerToken = getEncryptedBearerToken();
            String payload = buildC2bPayload(request);

            Request httpRequest = new Request.Builder()
                    .url(mpesaProperties.getApiBaseUrl() + "/ipg/v2/vodacomTZN/c2bPayment/singleStage")
                    .addHeader("Authorization", "Bearer " + bearerToken)
                    .addHeader("Origin", "*")
                    .post(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("M-Pesa C2B response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String conversationId = jsonResponse.path("output_ConversationID").asText();
                String responseCode = jsonResponse.path("output_ResponseCode").asText();
                String responseDesc = jsonResponse.path("output_ResponseDesc").asText();

                request.setProviderConversationId(conversationId);
                request.setProviderResponseCode(responseCode);
                request.setProviderResponseMessage(responseDesc);

                if ("INS-0".equals(responseCode)) {
                    request.setStatus(PaymentRequestStatus.SENT);
                } else {
                    request.setStatus(PaymentRequestStatus.FAILED);
                    request.setFailureReason(responseDesc);
                }
            }
        } catch (Exception e) {
            log.error("M-Pesa C2B request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest initiateDisbursement(PaymentRequest request) {
        if (!mpesaProperties.isEnabled()) {
            return simulateDisbursement(request);
        }

        try {
            String bearerToken = getEncryptedBearerToken();
            String payload = buildB2cPayload(request);

            Request httpRequest = new Request.Builder()
                    .url(mpesaProperties.getApiBaseUrl() + "/ipg/v2/vodacomTZN/b2cPayment/singleStage")
                    .addHeader("Authorization", "Bearer " + bearerToken)
                    .addHeader("Origin", "*")
                    .post(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("M-Pesa B2C response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String conversationId = jsonResponse.path("output_ConversationID").asText();
                String responseCode = jsonResponse.path("output_ResponseCode").asText();

                request.setProviderConversationId(conversationId);
                request.setProviderResponseCode(responseCode);
            }
        } catch (Exception e) {
            log.error("M-Pesa B2C request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest processCallback(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String conversationId = json.path("output_ConversationID").asText(
                    json.path("ConversationID").asText());
            String resultCode = json.path("output_ResultCode").asText(
                    json.path("ResultCode").asText());
            String transactionId = json.path("output_TransactionID").asText(
                    json.path("TransactionID").asText());

            PaymentRequest request = paymentRequestRepository
                    .findByProviderConversationId(conversationId)
                    .orElse(null);

            if (request == null) {
                log.warn("No payment request found for M-Pesa conversation ID: {}", conversationId);
                return null;
            }

            request.setProviderReference(transactionId);
            request.setCallbackPayload(payload);

            if ("0".equals(resultCode) || "INS-0".equals(resultCode)) {
                request.setStatus(PaymentRequestStatus.CALLBACK_RECEIVED);
            } else {
                request.setStatus(PaymentRequestStatus.FAILED);
                request.setFailureReason("M-Pesa result code: " + resultCode);
            }

            return request;
        } catch (Exception e) {
            log.error("Failed to parse M-Pesa callback: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PaymentRequest queryTransactionStatus(String providerReference) {
        return paymentRequestRepository.findByProviderReference(providerReference).orElse(null);
    }

    @Override
    public boolean isAvailable() {
        return mpesaProperties.isEnabled() || true; // Always available in mock mode
    }

    // --- Mock/simulation methods for development ---

    private PaymentRequest simulateCollection(PaymentRequest request) {
        log.info("MOCK: Simulating M-Pesa C2B collection for {} TZS {} to {}",
                request.getPhoneNumber(), request.getAmount(), request.getRequestNumber());
        request.setProviderConversationId("MOCK-CONV-" + System.currentTimeMillis());
        request.setProviderResponseCode("INS-0");
        request.setProviderResponseMessage("Mock: Request accepted successfully");
        request.setStatus(PaymentRequestStatus.SENT);

        // Simulate async callback (in real implementation, M-Pesa sends this)
        simulateCallbackAsync(request);
        return request;
    }

    private PaymentRequest simulateDisbursement(PaymentRequest request) {
        log.info("MOCK: Simulating M-Pesa B2C disbursement for {} TZS {} to {}",
                request.getPhoneNumber(), request.getAmount(), request.getRequestNumber());
        request.setProviderConversationId("MOCK-CONV-" + System.currentTimeMillis());
        request.setProviderResponseCode("INS-0");
        request.setProviderResponseMessage("Mock: Disbursement initiated");
        request.setStatus(PaymentRequestStatus.SENT);

        simulateCallbackAsync(request);
        return request;
    }

    private void simulateCallbackAsync(PaymentRequest request) {
        callbackSimulator.simulateSuccessCallback(request, PaymentProvider.MPESA);
    }

    // --- Helper methods ---

    private String getEncryptedBearerToken() {
        return MpesaEncryptionUtil.encryptApiKey(
                mpesaProperties.getApiKey(),
                mpesaProperties.getPublicKey()
        );
    }

    private String buildC2bPayload(PaymentRequest request) {
        return String.format("""
                {
                    "input_TransactionReference": "%s",
                    "input_CustomerMSISDN": "%s",
                    "input_Amount": "%s",
                    "input_ThirdPartyConversationID": "%s",
                    "input_ServiceProviderCode": "%s",
                    "input_PurchasedItemsDesc": "%s"
                }
                """,
                request.getRequestNumber(),
                formatPhoneNumber(request.getPhoneNumber()),
                request.getAmount().toPlainString(),
                "TP-" + request.getRequestNumber(),
                mpesaProperties.getServiceProviderCode(),
                request.getPurpose() != null ? request.getPurpose() : "SACCOS Payment"
        );
    }

    private String buildB2cPayload(PaymentRequest request) {
        return String.format("""
                {
                    "input_TransactionReference": "%s",
                    "input_CustomerMSISDN": "%s",
                    "input_Amount": "%s",
                    "input_ThirdPartyConversationID": "%s",
                    "input_ServiceProviderCode": "%s",
                    "input_Country": "TZN",
                    "input_Currency": "TZS"
                }
                """,
                request.getRequestNumber(),
                formatPhoneNumber(request.getPhoneNumber()),
                request.getAmount().toPlainString(),
                "TP-" + request.getRequestNumber(),
                mpesaProperties.getServiceProviderCode()
        );
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "255" + cleaned.substring(1);
        }
        if (cleaned.startsWith("255")) {
            return cleaned;
        }
        return "255" + cleaned;
    }
}
