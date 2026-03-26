package com.sor.core.domain;

/**
 * 订单方向
 */
public enum OrderSide {
    BUY((byte) 1),
    SELL((byte) 2);

    private final byte code;

    OrderSide(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static OrderSide fromCode(byte code) {
        return switch (code) {
            case 1 -> BUY;
            case 2 -> SELL;
            default -> throw new IllegalArgumentException("Invalid order side code: " + code);
        };
    }
}
