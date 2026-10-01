package com.orderdesk.payment;

import java.math.BigDecimal;

/** External card processor. */
public interface PaymentGateway {

    /** @return processor reference of the captured charge */
    String charge(String cardNumber, BigDecimal amount) throws PaymentDeclinedException;
}
