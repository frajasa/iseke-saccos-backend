package tz.co.iseke.controller.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.service.payment.PaymentOrchestrationService;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentOrchestrationService paymentOrchestrationService;

    @PostMapping("/mpesa/c2b/callback")
    public ResponseEntity<Map<String, String>> mpesaC2bCallback(@RequestBody String payload) {
        log.info("Received M-Pesa C2B callback");
        return processCallback(PaymentProvider.MPESA, payload);
    }

    @PostMapping("/mpesa/b2c/callback")
    public ResponseEntity<Map<String, String>> mpesaB2cCallback(@RequestBody String payload) {
        log.info("Received M-Pesa B2C callback");
        return processCallback(PaymentProvider.MPESA, payload);
    }

    @PostMapping("/mpesa/timeout")
    public ResponseEntity<Map<String, String>> mpesaTimeout(@RequestBody String payload) {
        log.info("Received M-Pesa timeout callback");
        return processCallback(PaymentProvider.MPESA, payload);
    }

    @PostMapping("/tigopesa/callback")
    public ResponseEntity<Map<String, String>> tigopesaCallback(@RequestBody String payload) {
        log.info("Received Tigopesa callback");
        return processCallback(PaymentProvider.TIGOPESA, payload);
    }

    @PostMapping("/nmb/callback")
    public ResponseEntity<Map<String, String>> nmbCallback(@RequestBody String payload) {
        log.info("Received NMB Bank callback");
        return processCallback(PaymentProvider.NMB_BANK, payload);
    }

    private ResponseEntity<Map<String, String>> processCallback(PaymentProvider provider, String payload) {
        try {
            PaymentRequest request = paymentOrchestrationService.handleCallback(provider, payload);
            if (request != null) {
                log.info("Callback processed for payment request: {} status: {}",
                        request.getRequestNumber(), request.getStatus());
            }
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            log.error("Error processing {} callback: {}", provider, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
