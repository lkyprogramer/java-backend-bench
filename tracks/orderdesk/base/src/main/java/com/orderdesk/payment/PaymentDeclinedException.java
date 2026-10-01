package com.orderdesk.payment;

/** The processor refused the charge; nothing was captured. */
public class PaymentDeclinedException extends Exception {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
