package com.sor.core.domain;

/**
 * 订单类型
 */
public enum OrderType {
    MARKET((byte) 1),      // 市价单
    LIMIT((byte) 2),       // 限价单
    STOP_LOSS((byte) 3),   // 止损单
    STOP_LIMIT((byte) 4);  // 止损限价单

    private final byte code;

    OrderType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static OrderType fromCode(byte code) {
        return switch (code) {
            case 1 -> MARKET;
            case 2 -> LIMIT;
            case 3 -> STOP_LOSS;
            case 4 -> STOP_LIMIT;
            default -> throw new IllegalArgumentException("Invalid order type code: " + code);
        };
    }
}
