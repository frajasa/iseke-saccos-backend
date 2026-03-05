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
import tz.co.iseke.enums.PaymentProvider;

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

    private String providerConversationId;
    private String providerReference;
    private String requestNumber;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        providerConversationId = "MOCK-CONV-12345";
        providerReference = "MOCK-REF-12345";
        requestNumber = "PAY123456";
        amount = new BigDecimal("100000");
    }

    @Test
    void mpesaPayload_containsCorrectFields() throws Exception {
        String payload = simulator.buildCallbackPayload(providerConversationId, providerReference,
                requestNumber, amount, PaymentProvider.MPESA);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-CONV-12345", json.path("output_ConversationID").asText());
        assertEquals("INS-0", json.path("output_ResponseCode").asText());
        assertEquals("0", json.path("output_ResultCode").asText());
        assertTrue(json.has("output_TransactionID"));
    }

    @Test
    void tigopesaPayload_containsCorrectFields() throws Exception {
        String payload = simulator.buildCallbackPayload(providerConversationId, providerReference,
                requestNumber, amount, PaymentProvider.TIGOPESA);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-REF-12345", json.path("transaction_ref_id").asText());
        assertEquals("success", json.path("trans_status").asText());
        assertEquals("100000", json.path("amount").asText());
        assertTrue(json.has("external_ref_id"));
    }

    @Test
    void nmbPayload_containsCorrectFields() throws Exception {
        String payload = simulator.buildCallbackPayload(providerConversationId, providerReference,
                requestNumber, amount, PaymentProvider.NMB_BANK);
        JsonNode json = objectMapper.readTree(payload);

        assertEquals("MOCK-REF-12345", json.path("transactionReference").asText());
        assertEquals("SUCCESS", json.path("status").asText());
        assertTrue(json.has("transactionId"));
    }
}
