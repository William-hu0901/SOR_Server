package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * VWAP 算法单元测试
 */
@DisplayName("VWAP 算法测试")
class VWAPAlgorithmTest {
    
    @Test
    @DisplayName("测试基础 VWAP 计算")
    void testBasicVWAPCalculation() {
        VWAPAlgorithm vwap = new VWAPAlgorithm();
        
        List<VWAPAlgorithm.Trade> trades = new ArrayList<>();
        trades.add(new VWAPAlgorithm.Trade(100.0, 1000, System.nanoTime()));
        trades.add(new VWAPAlgorithm.Trade(101.0, 2000, System.nanoTime() + 1000));
        trades.add(new VWAPAlgorithm.Trade(99.0, 1000, System.nanoTime() + 2000));
        
        VWAPAlgorithm.VWAPResult result = vwap.calculate(trades);
        
        // 期望 VWAP = (100*1000 + 101*2000 + 99*1000) / (1000+2000+1000)
        // = (100000 + 202000 + 99000) / 4000 = 401000 / 4000 = 100.25
        assertEquals(100.25, result.getVWAP(), 0.01);
        assertEquals(4000, result.getTotalVolume());
        assertEquals(3, result.getTradeCount());
    }
    
    @Test
    @DisplayName("测试增量 VWAP 计算器")
    void testIncrementalCalculator() {
        VWAPAlgorithm.IncrementalVWAPCalculator calculator = 
            new VWAPAlgorithm.IncrementalVWAPCalculator();
        
        calculator.addTrade(100.0, 500, System.nanoTime());
        calculator.addTrade(102.0, 300, System.nanoTime() + 1000);
        
        assertEquals(100.75, calculator.getVWAP(), 0.01);
        // (100*500 + 102*300) / (500+300) = (50000 + 30600) / 800 = 100.75
        
        calculator.addTrade(98.0, 200, System.nanoTime() + 2000);
        assertEquals(100.2, calculator.getVWAP(), 0.01);
        // (50000 + 30600 + 19600) / 1000 = 100.2
    }
    
    @Test
    @DisplayName("测试执行质量评估")
    void testExecutionQuality() {
        VWAPAlgorithm vwap = new VWAPAlgorithm();
        
        // 买入场景：实际成交价低于 VWAP 为好
        double quality1 = vwap.evaluateExecutionQuality(99.0, 100.0, true);
        assertEquals(1.0, quality1, 0.1);  // 优于 VWAP 1%
        
        // 卖出场景：实际成交价高于 VWAP 为好
        double quality2 = vwap.evaluateExecutionQuality(101.0, 100.0, false);
        assertEquals(1.0, quality2, 0.1);  // 优于 VWAP 1%
    }
}
