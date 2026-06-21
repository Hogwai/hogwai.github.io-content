package com.hogwai.enums;

import lombok.Getter;

@Getter
public enum StepEnum {
    VALIDATE_ORDER_STEP("validateOrder"),
    CHECK_INVENTORY_STEP("checkInventory"),
    PROCESS_PAYMENT_STEP("processPayment"),
    APPLY_DISCOUNT_STEP("applyDiscount"),
    CALCULATE_TAX_STEP("calculateTax"),
    FULFILL_ORDER_STEP("fulfillOrder"),
    SEND_CONFIRMATION_STEP("sendConfirmation"),
    UPDATE_ACCOUNTING_STEP("updateAccounting"),
    ESCALATE_ORDER_STEP("escalateOrder"),
    ARCHIVE_ORDER_STEP("archiveOrder");

    private final String pattern;

    StepEnum(String pattern) {
        this.pattern = pattern;
    }
}
