package tz.co.iseke.service.payment.nmb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import tz.co.iseke.config.NmbBankProperties;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.service.payment.PaymentGateway;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NmbBankGateway implements PaymentGateway {

    private final NmbBankProperties nmbProperties;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.NMB_BANK;
    }

    @Override
    public PaymentRequest initiateCollection(PaymentRequest request) {
        if (!nmbProperties.isEnabled()) {
            return simulateCollection(request);
        }

        try {
            String accessToken = getAccessToken();
            String payload = buildCollectionPayload(request);

            Request httpRequest = new Request.Builder()
                    .url(nmbProperties.getApiBaseUrl() + "/api/v1/collections/create")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("NMB collection response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String referenceId = jsonResponse.path("transactionReference").asText();
                String status = jsonResponse.path("status").asText();

                request.setProviderReference(referenceId);
                request.setProviderResponseCode(status);

                if ("ACCEPTED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                    request.setStatus(PaymentRequestStatus.SENT);
                } else {
                    request.setStatus(PaymentRequestStatus.FAILED);
                    request.setFailureReason("NMB response: " + status);
                }
            }
        } catch (Exception e) {
            log.error("NMB collection request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest initiateDisbursement(PaymentRequest request) {
        if (!nmbProperties.isEnabled()) {
            return simulateDisbursement(request);
        }

        try {
            String accessToken = getAccessToken();
            String payload = buildDisbursementPayload(request);

            Request httpRequest = new Request.Builder()
                    .url(nmbProperties.getApiBaseUrl() + "/api/v1/transfers/create")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("NMB disbursement response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String referenceId = jsonResponse.path("transactionReference").asText();
                request.setProviderReference(referenceId);
            }
        } catch (Exception e) {
            log.error("NMB disbursement request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest processCallback(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String referenceId = json.path("transactionReference").asText();
            String status = json.path("status").asText();
            String transactionId = json.path("transactionId").asText();

            PaymentRequest request = paymentRequestRepository
                    .findByProviderReference(referenceId)
                    .orElse(null);

            if (request == null) {
                log.warn("No payment request found for NMB reference: {}", referenceId);
                return null;
            }

            request.setProviderConversationId(transactionId);
            request.setCallbackPayload(payload);

            if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                request.setStatus(PaymentRequestStatus.CALLBACK_RECEIVED);
            } else {
                request.setStatus(PaymentRequestStatus.FAILED);
                request.setFailureReason("NMB callback status: " + status);
            }

            return request;
        } catch (Exception e) {
            log.error("Failed to parse NMB callback: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PaymentRequest queryTransactionStatus(String providerReference) {
        return paymentRequestRepository.findByProviderReference(providerReference).orElse(null);
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available (real or mock mode)
    }

    @Override
    public boolean isMockMode() {
        return !nmbProperties.isEnabled();
    }

    // --- Mock methods ---

    private PaymentRequest simulateCollection(PaymentRequest request) {
        log.info("MOCK: Simulating NMB collection for account {} TZS {}",
                request.getBankAccountNumber(), request.getAmount());
        request.setProviderReference("NMB-MOCK-" + System.currentTimeMillis());
        request.setProviderResponseCode("ACCEPTED");
        request.setStatus(PaymentRequestStatus.SENT);
        return request;
    }

    private PaymentRequest simulateDisbursement(PaymentRequest request) {
        log.info("MOCK: Simulating NMB disbursement to account {} TZS {}",
                request.getBankAccountNumber(), request.getAmount());
        request.setProviderReference("NMB-MOCK-" + System.currentTimeMillis());
        request.setProviderResponseCode("ACCEPTED");
        request.setStatus(PaymentRequestStatus.SENT);
        return request;
    }

    // --- Helper methods ---

    private String getAccessToken() {
        try {
            String credentials = Credentials.basic(nmbProperties.getClientId(), nmbProperties.getClientSecret());

            Request request = new Request.Builder()
                    .url(nmbProperties.getApiBaseUrl() + "/oauth2/token")
                    .addHeader("Authorization", credentials)
                    .post(RequestBody.create("grant_type=client_credentials",
                            MediaType.parse("application/x-www-form-urlencoded")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                JsonNode json = objectMapper.readTree(body);
                return json.path("access_token").asText();
            }
        } catch (Exception e) {
            log.error("Failed to get NMB access token: {}", e.getMessage());
            throw new RuntimeException("NMB authentication failed", e);
        }
    }

    private String buildCollectionPayload(PaymentRequest request) {
        return String.format("""
                {
                    "sourceAccount": "%s",
                    "amount": "%s",
                    "currency": "TZS",
                    "reference": "%s",
                    "narration": "%s"
                }
                """,
                request.getBankAccountNumber() != null ? request.getBankAccountNumber() : "",
                request.getAmount().toPlainString(),
                request.getRequestNumber(),
                request.getPurpose() != null ? request.getPurpose() : "SACCOS Payment"
        );
    }

    private String buildDisbursementPayload(PaymentRequest request) {
        return String.format("""
                {
                    "sourceAccount": "%s",
                    "destinationAccount": "%s",
                    "amount": "%s",
                    "currency": "TZS",
                    "reference": "%s",
                    "narration": "Loan Disbursement"
                }
                """,
                nmbProperties.getAccountNumber(),
                request.getBankAccountNumber() != null ? request.getBankAccountNumber() : "",
                request.getAmount().toPlainString(),
                request.getRequestNumber()
        );
    }
}
