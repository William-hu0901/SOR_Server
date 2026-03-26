package com.sor.risk.benchmark;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import com.sor.risk.ComplianceEngine;
import com.sor.risk.FrequencyLimitChecker;
import com.sor.risk.PriceLimitChecker;
import com.sor.risk.QuantityLimitChecker;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Risk 检查器性能基准测试
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(1)
public class RiskCheckerBenchmark {
    
    private ComplianceEngine engine;
    private Order validOrder;
    private long orderIdCounter;
    
    @Setup
    public void setup() {
        // 创建合规引擎并添加所有检查器
        engine = new ComplianceEngine();
        engine.addChecker(new PriceLimitChecker(10.0));
        engine.addChecker(new QuantityLimitChecker(1, 10000));
        engine.addChecker(new FrequencyLimitChecker(1000, 5000, 10000));
        
        // 设置参考价格
        PriceLimitChecker priceChecker = new PriceLimitChecker(10.0);
        priceChecker.setReferencePrice(150.0);
        
        // 创建有效订单
        validOrder = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 150.0, System.nanoTime()
        );
        
        orderIdCounter = 0;
    }
    
    /**
     * 测试价格检查性能
     */
    @Benchmark
    public boolean testPriceCheck() {
        PriceLimitChecker checker = new PriceLimitChecker(10.0);
        checker.setReferencePrice(150.0);
        
        Order order = createOrder(150.0);
        return checker.check(order).isPassed();
    }
    
    /**
     * 测试数量检查性能
     */
    @Benchmark
    public boolean testQuantityCheck() {
        QuantityLimitChecker checker = new QuantityLimitChecker(1, 10000);
        
        Order order = createOrder(100);
        return checker.check(order).isPassed();
    }
    
    /**
     * 测试频率检查性能
     */
    @Benchmark
    public boolean testFrequencyCheck() {
        FrequencyLimitChecker checker = new FrequencyLimitChecker(1000, 5000, 10000);
        
        Order order = createOrder(System.nanoTime());
        return checker.check(order).isPassed();
    }
    
    /**
     * 测试完整合规检查性能
     */
    @Benchmark
    public boolean testFullComplianceCheck() {
        Order order = createOrder(150.0);
        return engine.check(order).isPassed();
    }
    
    /**
     * 辅助方法：创建订单
     */
    private Order createOrder(double price) {
        return new Order(
            orderIdCounter++,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            100,
            price,
            System.nanoTime()
        );
    }
    
    private Order createOrder(int quantity) {
        return new Order(
            orderIdCounter++,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            quantity,
            150.0,
            System.nanoTime()
        );
    }
}
