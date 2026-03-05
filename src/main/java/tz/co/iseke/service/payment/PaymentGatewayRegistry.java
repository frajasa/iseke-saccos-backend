package tz.co.iseke.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tz.co.iseke.enums.PaymentProvider;
import tz.co.iseke.exception.BusinessException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PaymentGatewayRegistry {

    private final Map<PaymentProvider, PaymentGateway> gateways = new EnumMap<>(PaymentProvider.class);

    public PaymentGatewayRegistry(List<PaymentGateway> gatewayList) {
        for (PaymentGateway gateway : gatewayList) {
            gateways.put(gateway.getProvider(), gateway);
            log.info("Registered payment gateway: {}", gateway.getProvider());
        }
    }

    public PaymentGateway getGateway(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new BusinessException("Payment provider not configured: " + provider);
        }
        return gateway;
    }

    public boolean isProviderAvailable(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        return gateway != null && gateway.isAvailable();
    }

    public List<PaymentProvider> getAvailableProviders() {
        return gateways.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .toList();
    }
}
