package com.sor.core.domain;

import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles;

/**
 * 订单领域模型
 * 
 * 使用 VarHandle 实现无锁状态更新，避免重量级锁
 * 设计为可变对象以减少对象创建（通过重置复用）
 */
public final class Order {
    
    // VarHandle 用于原子操作 volatile 字段
    private static final VarHandle statusHandle;
    private static final VarHandle filledQuantityHandle;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            statusHandle = lookup.findVarHandle(Order.class, "status", int.class);
            filledQuantityHandle = lookup.findVarHandle(Order.class, "filledQuantity", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    // 订单基本属性（final，创建后不可变）
    private final long orderId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final int quantity;
    private final double price;  // 限价单价格，市价单为 0
    
    // 可变状态（使用 VarHandle 进行原子更新）
    private volatile int status;  // OrderStatus 的 code 值
    private volatile int filledQuantity;  // 已成交数量
    private volatile long timestamp;  // 订单时间戳（纳秒精度）
    
    // 路由信息
    private int targetExchangeId;  // 目标交易所 ID
    private long executionId;      // 执行 ID
    
    /**
     * 构造函数
     */
    public Order(long orderId, String symbol, OrderSide side, OrderType type,
                 int quantity, double price, long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
        this.status = OrderStatus.NEW.getCode();
        this.filledQuantity = 0;
        this.targetExchangeId = -1;
        this.executionId = 0;
    }
    
    // ==================== Getter 方法 ====================
    
    public long getOrderId() {
        return orderId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public OrderSide getSide() {
        return side;
    }
    
    public OrderType getType() {
        return type;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getPrice() {
        return price;
    }
    
    public OrderStatus getStatus() {
        return OrderStatus.fromCode((byte) statusHandle.get(this));
    }
    
    public int getFilledQuantity() {
        return (int) filledQuantityHandle.get(this);
    }
    
    public int getRemainingQuantity() {
        return quantity - getFilledQuantity();
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getTargetExchangeId() {
        return targetExchangeId;
    }
    
    public long getExecutionId() {
        return executionId;
    }
    
    // ==================== 原子更新方法（使用 VarHandle） ====================
    
    /**
     * CAS 更新订单状态
     * @param expected 期望状态
     * @param newState 新状态
     * @return 是否更新成功
     */
    public boolean compareAndSetStatus(OrderStatus expected, OrderStatus newState) {
        return statusHandle.compareAndSet(this, expected.getCode(), newState.getCode());
    }
    
    /**
     * 原子增加已成交数量
     * @param delta 增加的数量
     * @return 更新后的数量
     */
    public int addFilledQuantity(int delta) {
        int oldVal, newVal;
        do {
            oldVal = (int) filledQuantityHandle.get(this);
            newVal = oldVal + delta;
        } while (!filledQuantityHandle.compareAndSet(this, oldVal, newVal));
        return newVal;
    }
    
    /**
     * 设置目标交易所
     */
    public void setTargetExchange(int exchangeId) {
        this.targetExchangeId = exchangeId;
    }
    
    /**
     * 设置执行 ID
     */
    public void setExecutionId(long executionId) {
        this.executionId = executionId;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查订单是否完全成交
     */
    public boolean isFilled() {
        return getFilledQuantity() >= quantity;
    }
    
    /**
     * 检查订单是否可以撤销
     */
    public boolean isCancellable() {
        OrderStatus status = getStatus();
        return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED;
    }
    
    @Override
    public String toString() {
        return String.format("Order{id=%d, symbol=%s, side=%s, type=%s, qty=%d, price=%.2f, status=%s}",
                orderId, symbol, side, type, quantity, price, getStatus());
    }
}
