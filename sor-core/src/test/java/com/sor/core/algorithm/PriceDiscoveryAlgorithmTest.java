package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 价格发现算法单元测试
 */
@DisplayName("价格发现算法测试")
class PriceDiscoveryAlgorithmTest {
    
    @Test
    @DisplayName("测试公允价值计算")
    void testFairValueCalculation() {
        PriceDiscoveryAlgorithm priceDiscovery = new PriceDiscoveryAlgorithm();
        
        // 创建订单簿
        List<PriceDiscoveryAlgorithm.OrderBookItem> bids = new ArrayList<>();
        bids.add(new PriceDiscoveryAlgorithm.OrderBookItem(99.5, 1000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.BID));
        bids.add(new PriceDiscoveryAlgorithm.OrderBookItem(99.4, 2000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.BID));
        
        List<PriceDiscoveryAlgorithm.OrderBookItem> asks = new ArrayList<>();
        asks.add(new PriceDiscoveryAlgorithm.OrderBookItem(100.5, 1000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.ASK));
        asks.add(new PriceDiscoveryAlgorithm.OrderBookItem(100.6, 2000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.ASK));
        
        PriceDiscoveryAlgorithm.PriceDiscoveryResult result = 
            priceDiscovery.discoverPrice(bids, asks);
        
        // 中间价应该是 (99.5 + 100.5) / 2 = 100.0
        assertEquals(100.0, result.getFairValue(), 0.5);
        assertTrue(result.getConfidence() > 0.0);
        assertTrue(result.getConfidence() <= 1.0);
    }
    
    @Test
    @DisplayName("测试买卖不平衡度计算")
    void testImbalanceCalculation() {
        PriceDiscoveryAlgorithm priceDiscovery = new PriceDiscoveryAlgorithm();
        
        // 买方力量远大于卖方
        List<PriceDiscoveryAlgorithm.OrderBookItem> strongBids = new ArrayList<>();
        strongBids.add(new PriceDiscoveryAlgorithm.OrderBookItem(99.5, 10000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.BID));
        
        List<PriceDiscoveryAlgorithm.OrderBookItem> weakAsks = new ArrayList<>();
        weakAsks.add(new PriceDiscoveryAlgorithm.OrderBookItem(100.5, 1000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.ASK));
        
        PriceDiscoveryAlgorithm.PriceDiscoveryResult result = 
            priceDiscovery.discoverPrice(strongBids, weakAsks);
        
        // 不平衡度应该为正（买压强大）
        assertTrue(result.getImbalance() > 0);
    }
    
    @Test
    @DisplayName("测试套利机会检测")
    void testArbitrageDetection() {
        PriceDiscoveryAlgorithm priceDiscovery = new PriceDiscoveryAlgorithm();
        
        List<PriceDiscoveryAlgorithm.OrderBookItem> bids = new ArrayList<>();
        bids.add(new PriceDiscoveryAlgorithm.OrderBookItem(99.0, 1000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.BID));
        
        List<PriceDiscoveryAlgorithm.OrderBookItem> asks = new ArrayList<>();
        asks.add(new PriceDiscoveryAlgorithm.OrderBookItem(101.0, 1000, 
            PriceDiscoveryAlgorithm.OrderBookItem.Side.ASK));
        
        PriceDiscoveryAlgorithm.PriceDiscoveryResult result = 
            priceDiscovery.discoverPrice(bids, asks);
        
        // 假设市场价格为 105（显著高于公允价值 ~100）
        double marketPrice = 105.0;
        double signal = priceDiscovery.detectArbitrageOpportunity(marketPrice, result, 0.03);
        
        // 应该返回负值（做空信号）
        assertTrue(signal < 0);
    }
}
