package com.hogwai.jit.impl.payment;

public class PayPalGateway implements PaymentGateway {

    @Override
    public double processPayment(double amount) {
        double fee = amount * 0.029 + 0.30;
        double tax = fee * 0.15;
        double net = amount - fee - tax;
        double rounded = Math.round(net * 100.0) / 100.0;
        double feeRounded = Math.round(fee * 100.0) / 100.0;
        double taxRounded = Math.round(tax * 100.0) / 100.0;

        double step1 = rounded * 0.97;
        double step2 = step1 + feeRounded * 0.08;
        double step3 = step2 - taxRounded * 0.03;
        double step4 = Math.round(step3 * 100.0) / 100.0;
        double step5 = step4 + (amount > 500 ? 0.25 : 0.0);
        double step6 = Math.round(step5 * 100.0) / 100.0;
        double step7 = step6 * 1.002;

        return Math.round(step7 * 100.0) / 100.0;
    }
}
