package tz.co.iseke.service.payment.tigopesa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import tz.co.iseke.config.TigopesaProperties;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.enums.PaymentRequestStatus;
import tz.co.iseke.repository.PaymentRequestRepository;
import tz.co.iseke.service.payment.PaymentGateway;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TigopesaGateway implements PaymentGateway {

    private final TigopesaProperties tigopesaProperties;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM_MEDIA = MediaType.parse("application/x-www-form-urlencoded");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.TIGOPESA;
    }

    @Override
    public PaymentRequest initiateCollection(PaymentRequest request) {
        if (!tigopesaProperties.isEnabled()) {
            return simulateCollection(request);
        }

        try {
            String accessToken = getAccessToken();
            String payload = buildPaymentAuthPayload(request);

            String url = tigopesaProperties.getApiBaseUrl()
                    + "/v1/tigo/payment-auth" + getGatewayMode() + "/authorize";

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("accessToken", accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("Tigopesa payment-auth response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String redirectUrl = jsonResponse.path("redirectUrl").asText(null);
                String referenceId = jsonResponse.path("referenceId").asText(null);

                request.setProviderReference(referenceId);

                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    request.setProviderResponseMessage(redirectUrl);
                    request.setProviderResponseCode("success");
                    request.setStatus(PaymentRequestStatus.SENT);
                } else {
                    request.setStatus(PaymentRequestStatus.FAILED);
                    request.setFailureReason("Unable to get response from Tigopesa");
                }
            }
        } catch (Exception e) {
            log.error("Tigopesa collection request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest initiateDisbursement(PaymentRequest request) {
        if (!tigopesaProperties.isEnabled()) {
            return simulateDisbursement(request);
        }

        try {
            String accessToken = getAccessToken();
            String payload = buildDisbursementPayload(request);

            String url = tigopesaProperties.getApiBaseUrl()
                    + "/v1/tigo/payment/disbursement" + getGatewayMode();

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("accessToken", accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(payload, JSON_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("Tigopesa disbursement response: {}", responseBody);

                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String referenceId = jsonResponse.path("referenceId").asText(null);
                String status = jsonResponse.path("responseStatus").asText("");

                request.setProviderReference(referenceId);
                request.setProviderResponseCode(status);

                if ("success".equalsIgnoreCase(status)) {
                    request.setStatus(PaymentRequestStatus.SENT);
                } else {
                    request.setStatus(PaymentRequestStatus.FAILED);
                    request.setFailureReason("Tigopesa disbursement response: " + status);
                }
            }
        } catch (Exception e) {
            log.error("Tigopesa disbursement request failed: {}", e.getMessage());
            request.setStatus(PaymentRequestStatus.FAILED);
            request.setFailureReason(e.getMessage());
        }

        return request;
    }

    @Override
    public PaymentRequest processCallback(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            // Tigopesa callback uses transaction_ref_id and trans_status
            String transactionRefId = json.path("transaction_ref_id").asText(null);
            String transStatus = json.path("trans_status").asText("");
            String externalRefId = json.path("external_ref_id").asText(null);

            if (transactionRefId == null || transactionRefId.isEmpty()) {
                log.warn("Tigopesa callback missing transaction_ref_id");
                return null;
            }

            // Extract request number from ref id (format: requestNumber-uniqueId)
            String requestNumber = transactionRefId.contains("-")
                    ? transactionRefId.substring(0, transactionRefId.indexOf('-'))
                    : transactionRefId;

            PaymentRequest request = paymentRequestRepository
                    .findByProviderReference(transactionRefId)
                    .or(() -> paymentRequestRepository.findByRequestNumber(requestNumber))
                    .orElse(null);

            if (request == null) {
                log.warn("No payment request found for Tigopesa ref: {}", transactionRefId);
                return null;
            }

            request.setProviderConversationId(externalRefId);
            request.setCallbackPayload(payload);

            if ("success".equalsIgnoreCase(transStatus)) {
                request.setStatus(PaymentRequestStatus.CALLBACK_RECEIVED);
            } else {
                request.setStatus(PaymentRequestStatus.FAILED);
                request.setFailureReason("Tigopesa callback status: " + transStatus);
            }

            return request;
        } catch (Exception e) {
            log.error("Failed to parse Tigopesa callback: {}", e.getMessage());
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
        return !tigopesaProperties.isEnabled();
    }

    // --- Mock methods ---

    private PaymentRequest simulateCollection(PaymentRequest request) {
        log.info("MOCK: Simulating Tigopesa collection for {} TZS {}", request.getPhoneNumber(), request.getAmount());
        request.setProviderReference("TIGO-MOCK-" + System.currentTimeMillis());
        request.setProviderResponseCode("success");
        request.setStatus(PaymentRequestStatus.SENT);
        return request;
    }

    private PaymentRequest simulateDisbursement(PaymentRequest request) {
        log.info("MOCK: Simulating Tigopesa disbursement for {} TZS {}", request.getPhoneNumber(), request.getAmount());
        request.setProviderReference("TIGO-MOCK-" + System.currentTimeMillis());
        request.setProviderResponseCode("success");
        request.setStatus(PaymentRequestStatus.SENT);
        return request;
    }

    // --- Helper methods ---

    private String getAccessToken() {
        try {
            String url = tigopesaProperties.getApiBaseUrl()
                    + "/v1/oauth/generate/accesstoken" + getGatewayMode()
                    + "?grant_type=client_credentials";

            String formBody = "client_id=" + tigopesaProperties.getClientId()
                    + "&client_secret=" + tigopesaProperties.getClientSecret();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(RequestBody.create(formBody, FORM_MEDIA))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                JsonNode json = objectMapper.readTree(body);

                String accessToken = json.path("accessToken").asText(null);
                if (accessToken != null && !accessToken.isEmpty()) {
                    return accessToken;
                }

                String error = json.path("Error").asText("Unknown authentication error");
                throw new RuntimeException("Tigopesa auth error: " + error);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get Tigopesa access token: {}", e.getMessage());
            throw new RuntimeException("Tigopesa authentication failed", e);
        }
    }

    private String buildPaymentAuthPayload(PaymentRequest request) throws Exception {
        String amount = request.getAmount().toPlainString();
        String currency = tigopesaProperties.getCurrency();
        String phone = formatPhoneNumber(request.getPhoneNumber());
        String countryCode = "255";
        String country = "TZA";

        Map<String, Object> payload = Map.ofEntries(
                Map.entry("MasterMerchant", Map.of(
                        "account", tigopesaProperties.getAccount(),
                        "pin", tigopesaProperties.getPin(),
                        "id", tigopesaProperties.getMerchantName()
                )),
                Map.entry("Merchant", Map.of(
                        "reference", "",
                        "fee", "0",
                        "currencyCode", ""
                )),
                Map.entry("Subscriber", Map.of(
                        "account", phone,
                        "countryCode", countryCode,
                        "country", country,
                        "firstName", request.getMember() != null ? request.getMember().getFirstName() : "",
                        "lastName", request.getMember() != null ? request.getMember().getLastName() : "",
                        "emailId", ""
                )),
                Map.entry("originPayment", Map.of(
                        "amount", amount,
                        "currencyCode", currency,
                        "tax", "0",
                        "fee", "0"
                )),
                Map.entry("LocalPayment", Map.of(
                        "amount", amount,
                        "currencyCode", currency
                )),
                Map.entry("redirectUri", tigopesaProperties.getRedirectUrl() != null ? tigopesaProperties.getRedirectUrl() : ""),
                Map.entry("callbackUri", tigopesaProperties.getCallbackUrl() != null ? tigopesaProperties.getCallbackUrl() : ""),
                Map.entry("language", tigopesaProperties.getLanguage()),
                Map.entry("terminalId", ""),
                Map.entry("exchangeRate", "1"),
                Map.entry("transactionRefId", request.getRequestNumber())
        );

        return objectMapper.writeValueAsString(payload);
    }

    private String buildDisbursementPayload(PaymentRequest request) throws Exception {
        Map<String, Object> payload = Map.of(
                "MasterMerchant", Map.of(
                        "account", tigopesaProperties.getAccount(),
                        "pin", tigopesaProperties.getPin(),
                        "id", tigopesaProperties.getMerchantName()
                ),
                "SubscriberMSISDN", formatPhoneNumber(request.getPhoneNumber()),
                "Amount", request.getAmount().toPlainString(),
                "ReferenceID", request.getRequestNumber()
        );

        return objectMapper.writeValueAsString(payload);
    }

    private String getGatewayMode() {
        return tigopesaProperties.isTestMode() ? "-test-2018" : "";
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
