package com.orderdesk.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Sandbox processor used in every environment of this project: cards ending in 0002 are declined,
 * every other 16-digit card is captured. Amounts must be positive.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public String charge(String cardNumber, BigDecimal amount) throws PaymentDeclinedException {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive");
        }
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            throw new PaymentDeclinedException("Invalid card number");
        }
        if (cardNumber.endsWith("0002")) {
            throw new PaymentDeclinedException("Card declined by issuer");
        }
        return "ch_" + UUID.randomUUID();
    }
}
