package com.hogwai.enums;

import lombok.Getter;

@Getter
public enum JobInputParameter {
    ID_PROCESS("idProcess"),
    TIME("time"),
    CONFIGURATION("chainConfigName"),
    INVOICE_STATUS("invoiceStatus");

    private final String key;

    JobInputParameter(String key) {
        this.key = key;
    }
}
