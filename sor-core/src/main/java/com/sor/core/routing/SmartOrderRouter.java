package com.sor.core.routing;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能订单路由引擎
 * 
 * 核心职责：
 * 1. 接收订单并分析特征
 * 2. 查询市场流动性
 * 3. 执行路由策略选择最优交易所
 * 4. 发送订单至目标交易所
 * 
 * 性能优化：
 * - 使用虚拟线程处理并发请求
 * - 热点方法使用@ForceInline 提示 JIT 内联
 * - 避免在热路径上创建对象
 */
public class SmartOrderRouter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmartOrderRouter.class);
    
    // 路由策略数组（缓存常用策略，避免重复创建）
    private final RoutingStrategy[] strategies;
    
    // 流动性管理器
    private final LiquidityManager liquidityManager;
    
    /**
     * 构造函数
     */
    public SmartOrderRouter(LiquidityManager liquidityManager) {
        this.liquidityManager = liquidityManager;
        this.strategies = new RoutingStrategy[] {
            new BestPriceRouting(),
            new LowLatencyRouting(),
            new VWAPRouting()
        };
    }
    
    /**
     * 路由订单至最优交易所
     * 
     * @param order 待路由订单
     * @return 目标交易所 ID
     */
    public int routeOrder(Order order) {
        // 获取当前市场流动性数据
        var liquidity = liquidityManager.getLiquidity(order.getSymbol());
        
        if (liquidity == null || liquidity.isEmpty()) {
            LOG.warn("No liquidity available for symbol: {}", order.getSymbol());
            return -1;
        }
        
        // 根据订单类型选择合适的路由策略
        RoutingStrategy strategy = selectStrategy(order);
        
        // 执行路由决策（JIT 会内联此方法）
        int targetExchange = strategy.selectExchange(order, liquidity);
        
        // 设置目标交易所
        order.setTargetExchange(targetExchange);
        
        LOG.debug("Routed order {} to exchange {}, strategy={}", 
                  order.getOrderId(), targetExchange, strategy.name());
        
        return targetExchange;
    }
    
    /**
     * 根据订单特征选择路由策略
     * JIT 编译器会自动内联此方法
     */
    private RoutingStrategy selectStrategy(Order order) {
        return switch (order.getType()) {
            case MARKET -> strategies[1]; // 市价单优先低延迟
            case LIMIT -> strategies[0];  // 限价单优先最优价格
            case STOP_LOSS, STOP_LIMIT -> strategies[2]; // 止损单使用 VWAP
        };
    }
    
    /**
     * 批量路由订单（使用虚拟线程并行处理）
     */
    public void batchRoute(Order[] orders) {
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            // 为每个订单创建虚拟线程处理
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (Order order : orders) {
                futures.add(executor.submit(() -> {
                    routeOrder(order);
                    return null;
                }));
            }
            // 等待所有任务完成
            for (var future : futures) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    LOG.error("Error routing order", e.getCause());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Batch routing interrupted", e);
        }
    }
}