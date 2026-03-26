package com.sor.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.sor.core.routing.LiquidityManager;
import com.sor.core.routing.SmartOrderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;

/**
 * Disruptor 配置和初始化
 * 
 * 创建并配置 Disruptor 实例，设置 RingBuffer、生产者、消费者
 */
public class DisruptorConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(DisruptorConfig.class);
    
    // RingBuffer 大小（必须是 2 的幂次）
    private static final int BUFFER_SIZE = 1024 * 1024; // 1M events
    
    // Disruptor 实例
    private final Disruptor<OrderEvent> disruptor;
    
    // RingBuffer（用于发布事件）
    private final RingBuffer<OrderEvent> ringBuffer;
    
    // 事件处理器引用
    private final OrderEventHandler handler;
    
    /**
     * 构造函数
     */
    public DisruptorConfig() {
        // 创建流动性管理器和路由引擎
        LiquidityManager liquidityManager = new LiquidityManager();
        SmartOrderRouter router = new SmartOrderRouter(liquidityManager);
        
        // 创建事件处理器
        this.handler = new OrderEventHandler(new OrderRouter(router));
        
        // 创建 Disruptor 实例
        // 使用虚拟线程作为事件处理器
        ThreadFactory threadFactory = r -> {
            Thread virtualThread = Thread.ofVirtual().start(r);
            return virtualThread;
        };
        
        this.disruptor = new Disruptor<OrderEvent>(
            new OrderEventFactory(),
            BUFFER_SIZE,
            threadFactory,
            ProducerType.MULTI,  // 支持多生产者
            new BlockingWaitStrategy()  // 阻塞等待策略（可配置为 BusySpin 以获得更低延迟）
        );
        
        // 设置事件处理器
        this.disruptor.handleEventsWith(handler);
        
        // 启动 Disruptor
        this.ringBuffer = disruptor.start();
        
        LOG.info("Disruptor started with buffer size: {}", BUFFER_SIZE);
    }
    
    /**
     * 获取 RingBuffer（用于发布事件）
     */
    public RingBuffer<OrderEvent> getRingBuffer() {
        return ringBuffer;
    }
    
    /**
     * 获取事件处理器（用于统计）
     */
    public OrderEventHandler getHandler() {
        return handler;
    }
    
    /**
     * 关闭 Disruptor
     */
    public void shutdown() {
        disruptor.shutdown();
        LOG.info("Disruptor shut down gracefully");
    }
}
