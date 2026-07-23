package com.hogwai.jit.impl.payment;

/**
 * Shared interface used by inlining and devirtualization demos.
 * Each implementation has a ~40-line processPayment method to
 * provide enough bytecode for inlining-threshold demos.
 */
public interface PaymentGateway {
    double processPayment(double amount);
}
