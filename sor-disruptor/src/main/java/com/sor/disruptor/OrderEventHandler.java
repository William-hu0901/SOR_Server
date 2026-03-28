package com.sor.disruptor;

import com.lmax.disruptor.EventHandler;
import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

/**
 * 订单事件处理器
 * 
 * 在 Disruptor 的工作线程中执行，处理订单路由逻辑
 */
public class OrderEventHandler implements EventHandler<OrderEvent> {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrderEventHandler.class);
    
    // 订单路由器（注入）
    private final OrderRouter router;
    
    // 处理计数器（使用 VarHandle 实现原子操作）
    private final LongAdder processedCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    
    /**
     * 构造函数
     */
    public OrderEventHandler(OrderRouter router) {
        this.router = router;
    }
    
    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            // 根据事件类型处理
            switch (event.getType()) {
                case NEW_ORDER -> handleNewOrder(event);
                case CANCEL_ORDER -> handleCancelOrder(event);
                case MODIFY_ORDER -> handleModifyOrder(event);
                case EXECUTION -> handleExecution(event);
            }
            
            // 更新计数器
            incrementProcessed();
            
        } catch (Exception e) {
            LOG.error("Error processing event: {}", event, e);
            incrementError();
            throw e;
        } finally {
            // 重置事件以便复用
            event.reset();
        }
    }
    
    /**
     * 处理新订单
     */
    @SuppressWarnings("unchecked")
    private void handleNewOrder(OrderEvent event) {
        Order order = (Order) event.getOrder();
        if (order != null) {
            router.route(order);
        }
    }
    
    /**
     * 处理撤销订单
     */
    @SuppressWarnings("unchecked")
    private void handleCancelOrder(OrderEvent event) {
        Order order = (Order) event.getOrder();
        if (order != null) {
            router.cancel(order);
        }
    }
    
    /**
     * 处理修改订单
     */
    @SuppressWarnings("unchecked")
    private void handleModifyOrder(OrderEvent event) {
        Order order = (Order) event.getOrder();
        if (order != null) {
            router.modify(order);
        }
    }
    
    /**
     * 处理执行回报
     */
    @SuppressWarnings("unchecked")
    private void handleExecution(OrderEvent event) {
        Order order = (Order) event.getOrder();
        if (order != null) {
            router.onExecution(order);
        }
    }
    
    // ==================== 统计方法 ====================
    
    private void incrementProcessed() {
        processedCount.increment();  // 分段累加，无锁
    }
    
    private void incrementError() {errorCount.increment(); }
    
    public long getProcessedCount() {return processedCount.sum();    }
    
    public long getErrorCount() {
        return errorCount.sum();
    }
}
