package com.sor.core.domain;

/**
 * 订单状态
 */
public enum OrderStatus {
    PENDING((byte) 0),     // 待处理
    NEW((byte) 1),         // 新订单
    PARTIALLY_FILLED((byte) 2),  // 部分成交
    FILLED((byte) 3),      // 完全成交
    CANCELLED((byte) 4),   // 已撤销
    REJECTED((byte) 5),    // 已拒绝
    EXPIRED((byte) 6);     // 已过期

    private final byte code;

    OrderStatus(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static OrderStatus fromCode(byte code) {
        return switch (code) {
            case 0 -> PENDING;
            case 1 -> NEW;
            case 2 -> PARTIALLY_FILLED;
            case 3 -> FILLED;
            case 4 -> CANCELLED;
            case 5 -> REJECTED;
            case 6 -> EXPIRED;
            default -> throw new IllegalArgumentException("Invalid order status code: " + code);
        };
    }
}
