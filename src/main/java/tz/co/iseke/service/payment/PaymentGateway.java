package tz.co.iseke.service.payment;

import tz.co.iseke.entity.PaymentRequest;
import tz.co.iseke.enums.PaymentProvider;

import java.util.Map;

public interface PaymentGateway {

    PaymentProvider getProvider();

    PaymentRequest initiateCollection(PaymentRequest request);

    PaymentRequest initiateDisbursement(PaymentRequest request);

    PaymentRequest processCallback(String payload);

    PaymentRequest queryTransactionStatus(String providerReference);

    boolean isAvailable();

    default Map<String, Object> getProviderInfo() {
        return Map.of(
                "provider", getProvider().name(),
                "available", isAvailable()
        );
    }
}
