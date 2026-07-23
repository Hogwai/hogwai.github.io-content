package com.hogwai.jit.impl.payment;

public class BankTransferGateway implements PaymentGateway {

    @Override
    public double processPayment(double amount) {
        double fee = 1.50;
        double tax = 0.0;
        double net = amount - fee - tax;
        double rounded = Math.round(net * 100.0) / 100.0;
        double feeRounded = Math.round(fee * 100.0) / 100.0;
        double taxRounded = Math.round(tax * 100.0) / 100.0;

        double step1 = rounded * 0.99;
        double step2 = step1 + feeRounded * 0.05;
        double step3 = step2 - taxRounded;
        double step4 = Math.round(step3 * 100.0) / 100.0;
        double step5 = step4 + (amount > 2000 ? 1.0 : 0.0);
        double step6 = Math.round(step5 * 100.0) / 100.0;
        double step7 = step6 * 1.0005;

        return Math.round(step7 * 100.0) / 100.0;
    }
}
