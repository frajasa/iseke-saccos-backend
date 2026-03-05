package tz.co.iseke.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.testutil.TestDataBuilder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MockPaymentCallbackSimulatorTest {

    @Mock
    private PaymentOrchestrationService paymentOrchestrationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MockPaymentCallbackSimulator simulator;

    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        request = TestDataBuilder.aPaymentRequest(PaymentProvider.MPESA, new BigDecimal("100000"));
        request.setProviderConversationId("MOCK-CONV-12345");
        request.setProviderReference("MOCK-REF-12345");
    }

    @Test
    void mpesaPayload_containsCorrectFields() throws Exception {
        String payload = simulator.buildCallbackPayload(request, PaymentProvider.MPESA);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-CONV-12345", json.path("output_ConversationID").asText());
        assertEquals("INS-0", json.path("output_ResponseCode").asText());
        assertEquals("0", json.path("output_ResultCode").asText());
        assertTrue(json.has("output_TransactionID"));
    }

    @Test
    void tigopesaPayload_containsCorrectFields() throws Exception {
        request.setProvider(PaymentProvider.TIGOPESA);
        String payload = simulator.buildCallbackPayload(request, PaymentProvider.TIGOPESA);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-REF-12345", json.path("transaction_ref_id").asText());
        assertEquals("success", json.path("trans_status").asText());
        assertEquals("100000", json.path("amount").asText());
        assertTrue(json.has("external_ref_id"));
    }

    @Test
    void nmbPayload_containsCorrectFields() throws Exception {
        request.setProvider(PaymentProvider.NMB_BANK);
        String payload = simulator.buildCallbackPayload(request, PaymentProvider.NMB_BANK);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-REF-12345", json.path("transactionReference").asText());
        assertEquals("SUCCESS", json.path("status").asText());
        assertTrue(json.has("transactionId"));
    }
}
