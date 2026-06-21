package com.hogwai.enums;

import lombok.Getter;

@Getter
public enum ChainStatusEnum {
    ACTIVE(1, "ACTIVE"),
    SUSPENDED(2, "SUSPENDED"),
    ENABLED(3, "ENABLED");

    private final Integer id;
    private final String code;

    ChainStatusEnum(Integer id, String code) {
        this.id = id;
        this.code = code;
    }
}
