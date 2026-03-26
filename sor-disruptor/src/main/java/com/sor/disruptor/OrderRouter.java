package com.sor.disruptor;

import com.sor.core.domain.Order;
import com.sor.core.routing.SmartOrderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单路由器
 * 
 * 封装实际的路由逻辑，供事件处理器调用
 */
public class OrderRouter {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrderRouter.class);
    
    // 智能订单路由引擎
    private final SmartOrderRouter router;
    
    /**
     * 构造函数
     */
    public OrderRouter(SmartOrderRouter router) {
        this.router = router;
    }
    
    /**
     * 路由订单
     */
    public void route(Order order) {
        try {
            int targetExchange = router.routeOrder(order);
            LOG.debug("Routed order {} to exchange {}", order.getOrderId(), targetExchange);
        } catch (Exception e) {
            LOG.error("Failed to route order: {}", order, e);
            throw e;
        }
    }
    
    /**
     * 撤销订单
     */
    public void cancel(Order order) {
        if (order.isCancellable()) {
            LOG.info("Cancelling order: {}", order.getOrderId());
            // TODO: 实现撤销逻辑
        }
    }
    
    /**
     * 修改订单
     */
    public void modify(Order order) {
        LOG.info("Modifying order: {}", order.getOrderId());
        // TODO: 实现修改逻辑
    }
    
    /**
     * 处理执行回报
     */
    public void onExecution(Order order) {
        LOG.info("Execution report for order {}: filled={}/{}", 
                order.getOrderId(), 
                order.getFilledQuantity(), 
                order.getQuantity());
    }
}
