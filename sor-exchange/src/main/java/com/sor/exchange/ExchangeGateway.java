package com.sor.exchange;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 交易所接口
 * 
 * 所有交易所适配器必须实现此接口
 */
public interface ExchangeGateway {
    
    /**
     * 连接交易所
     */
    void connect();
    
    /**
     * 断开连接
     */
    void disconnect();
    
    /**
     * 发送订单
     * @param order 订单
     * @return 交易所订单 ID
     */
    String sendOrder(Order order);
    
    /**
     * 撤销订单
     * @param orderId 订单 ID
     * @return 是否成功
     */
    boolean cancelOrder(String orderId);
    
    /**
     * 修改订单
     * @param orderId 订单 ID
     * @param newPrice 新价格
     * @param newQuantity 新数量
     * @return 是否成功
     */
    boolean modifyOrder(String orderId, double newPrice, int newQuantity);
    
    /**
     * 查询订单状态
     * @param orderId 订单 ID
     * @return 订单状态
     */
    OrderStatus queryOrder(String orderId);
    
    /**
     * 是否已连接
     */
    boolean isConnected();
    
    /**
     * 订单状态枚举
     */
    enum OrderStatus {
        NEW,           // 新订单
        PARTIALLY_FILLED,  // 部分成交
        FILLED,        // 完全成交
        CANCELLED,     // 已撤销
        REJECTED,      // 已拒绝
        UNKNOWN        // 未知
    }
}
