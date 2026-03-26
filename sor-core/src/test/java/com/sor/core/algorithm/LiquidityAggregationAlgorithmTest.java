package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 流动性聚合算法单元测试
 */
@DisplayName("流动性聚合算法测试")
class LiquidityAggregationAlgorithmTest {
    
    @Test
    @DisplayName("测试订单簿聚合")
    void testOrderBookAggregation() {
        LiquidityAggregationAlgorithm aggregator = new LiquidityAggregationAlgorithm();
        
        // 创建来自不同提供商的订单
        List<LiquidityAggregationAlgorithm.OrderItem> orders = new ArrayList<>();
        orders.add(new LiquidityAggregationAlgorithm.OrderItem("LP1", 99.5, 1000, 
            LiquidityAggregationAlgorithm.OrderItem.Side.BID, System.nanoTime()));
        orders.add(new LiquidityAggregationAlgorithm.OrderItem("LP2", 99.6, 1500, 
            LiquidityAggregationAlgorithm.OrderItem.Side.BID, System.nanoTime()));
        orders.add(new LiquidityAggregationAlgorithm.OrderItem("LP1", 100.1, 1200, 
            LiquidityAggregationAlgorithm.OrderItem.Side.ASK, System.nanoTime()));
        orders.add(new LiquidityAggregationAlgorithm.OrderItem("LP3", 100.0, 800, 
            LiquidityAggregationAlgorithm.OrderItem.Side.ASK, System.nanoTime()));
        
        LiquidityAggregationAlgorithm.AggregatedBook book = aggregator.aggregate(orders);
        
        // 验证买盘聚合（相同价格合并）
        assertEquals(99.6, book.getBestBid(), 0.01);
        
        // 验证卖盘聚合
        assertEquals(100.0, book.getBestAsk(), 0.01);
        
        // 验证价差
        assertEquals(0.4, book.getSpread(), 0.01);
    }
    
    @Test
    @DisplayName("测试最优执行路径查找")
    void testOptimalPathFinding() {
        LiquidityAggregationAlgorithm aggregator = new LiquidityAggregationAlgorithm();
        
        // 创建聚合订单簿
        LiquidityAggregationAlgorithm.AggregatedBook book = 
            new LiquidityAggregationAlgorithm.AggregatedBook(System.nanoTime());
        book.addAsk(100.0, 500, "LP1");
        book.addAsk(100.1, 800, "LP2");
        book.addAsk(100.2, 1000, "LP3");
        
        // 创建流动性提供商信息
        List<LiquidityAggregationAlgorithm.LiquidityProvider> providers = new ArrayList<>();
        providers.add(createProvider("LP1", 0.001, 1.0));
        providers.add(createProvider("LP2", 0.002, 0.9));
        providers.add(createProvider("LP3", 0.0015, 0.95));
        
        // 查找买单（吃卖单）的最优路径
        List<LiquidityAggregationAlgorithm.ExecutionPath> paths = 
            aggregator.findOptimalPath(
                LiquidityAggregationAlgorithm.OrderItem.Side.BID,
                1000,  // 需要买入 1000 股
                book,
                providers
            );
        
        assertTrue(paths.size() > 0);
        // 应该优先选择 LP1（费用低、延迟低）
        assertEquals("LP1", paths.get(0).getProviderId());
    }
    
    private LiquidityAggregationAlgorithm.LiquidityProvider createProvider(
            String id, double fee, double reliability) {
        return new LiquidityAggregationAlgorithm.LiquidityProvider() {
            @Override
            public String getProviderId() { return id; }
            @Override
            public double getMakerFee() { return fee * 0.8; }
            @Override
            public double getTakerFee() { return fee; }
            @Override
            public double getLatency() { return 10.0; }
            @Override
            public double getReliability() { return reliability; }
        };
    }
}
