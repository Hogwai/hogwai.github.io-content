package com.hogwai.jit.impl.payment;

public class CreditCardGateway implements PaymentGateway {

    @Override
    public double processPayment(double amount) {
        double fee = amount * 0.025;
        double tax = fee * 0.2;
        double net = amount - fee - tax;
        double roundNet = Math.round(net * 100.0) / 100.0;
        double roundFee = Math.round(fee * 100.0) / 100.0;
        double roundTax = Math.round(tax * 100.0) / 100.0;

        double step1 = roundNet * 0.98;
        double step2 = step1 + roundFee * 0.1;
        double step3 = step2 - roundTax * 0.05;
        double step4 = Math.round(step3 * 100.0) / 100.0;
        double step5 = step4 + (amount > 1000 ? 0.5 : 0.0);
        double step6 = Math.round(step5 * 100.0) / 100.0;
        double step7 = step6 * 1.001;

        return Math.round(step7 * 100.0) / 100.0;
    }
}
