package com.sor.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * 订单路由性能基准测试
 * 
 * 测试目标：
 * - 单次路由延迟 < 50μs
 * - 吞吐量 > 1,000,000 TPS
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(1)
public class OrderRoutingBenchmark {
    
    // 测试数据
    private com.sor.core.domain.Order order;
    private com.sor.core.routing.LiquidityManager liquidityManager;
    private com.sor.core.routing.SmartOrderRouter router;
    
    @Setup
    public void setup() {
        // 初始化测试数据
        order = new com.sor.core.domain.Order(
            1L,
            "AAPL",
            com.sor.core.domain.OrderSide.BUY,
            com.sor.core.domain.OrderType.LIMIT,
            100,
            150.0,
            System.nanoTime()
        );
        
        liquidityManager = new com.sor.core.routing.LiquidityManager();
        liquidityManager.initMockLiquidity("AAPL");
        
        router = new com.sor.core.routing.SmartOrderRouter(liquidityManager);
    }
    
    /**
     * 测试单次订单路由延迟
     */
    @Benchmark
    public int testOrderRoutingLatency() {
        return router.routeOrder(order);
    }
    
    /**
     * 测试流动性查询性能
     */
    @Benchmark
    public Object testLiquidityQuery() {
        return liquidityManager.getLiquidity("AAPL");
    }
}
