package com.sor.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * 订单事件工厂
 * 
 * 仅在初始化时创建对象，之后通过重置复用，避免运行时分配
 */
public class OrderEvent implements EventFactory<OrderEvent> {
    
    // 订单 ID
    private long orderId;
    
    // 交易品种
    private String symbol;
    
    // 订单数据（引用，避免复制）
    private Object orderRef;
    
    // 时间戳（纳秒）
    private long timestamp;
    
    // 事件类型
    private EventType type;
    
    public enum EventType {
        NEW_ORDER,      // 新订单
        CANCEL_ORDER,   // 撤销订单
        MODIFY_ORDER,   // 修改订单
        EXECUTION       // 执行回报
    }
    
    /**
     * 默认构造函数（供 Disruptor 使用）
     */
    public OrderEvent() {
    }
    
    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
    
    /**
     * 初始化事件（热路径外调用）
     */
    public void initialize(long orderId, String symbol, Object order, EventType type) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.orderRef = order;
        this.type = type;
        this.timestamp = System.nanoTime();
    }
    
    /**
     * 重置事件（复用时调用）
     */
    public void reset() {
        this.orderId = 0;
        this.symbol = null;
        this.orderRef = null;
        this.type = null;
        this.timestamp = 0;
    }
    
    // ==================== Getter 方法 ====================
    
    public long getOrderId() {
        return orderId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getOrder() {
        return (T) orderRef;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public EventType getType() {
        return type;
    }
    
    /**
     * 计算延迟（当前时间与事件创建时间的差值）
     */
    public long getLatency() {
        return System.nanoTime() - timestamp;
    }
}
