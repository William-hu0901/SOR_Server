package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单分割算法单元测试
 */
@DisplayName("订单分割算法测试")
class OrderSlicingAlgorithmTest {
    
    @Test
    @DisplayName("测试等分策略")
    void testEqualSlicing() {
        OrderSlicingAlgorithm slicer = new OrderSlicingAlgorithm();
        
        OrderSlicingAlgorithm.SlicingConfig config = 
            new OrderSlicingAlgorithm.SlicingConfig(
                10000,  // 总数量 10000
                0.02,   // 最大冲击 2%
                100,    // 最小切片 100
                2000,   // 最大切片 2000
                OrderSlicingAlgorithm.SlicingStrategy.EQUAL,
                false,
                60      // 1 小时完成
            );
        
        OrderSlicingAlgorithm.MarketDepth depth = 
            new OrderSlicingAlgorithm.MarketDepth(5000, 5000, 0.01, 0.02);
        
        OrderSlicingAlgorithm.SlicingResult result = slicer.sliceOrder(config, depth);
        
        // 验证分割结果
        assertTrue(result.getNumSlices() > 1);
        assertTrue(result.getAvgSliceSize() >= 100);
        assertTrue(result.getAvgSliceSize() <= 2000);
        
        // 验证总数量守恒
        long totalQty = result.getSlices().stream()
            .mapToLong(OrderSlicingAlgorithm.OrderSlice::getQuantity)
            .sum();
        assertEquals(10000, totalQty);
    }
    
    @Test
    @DisplayName("测试自适应策略")
    void testAdaptiveSlicing() {
        OrderSlicingAlgorithm slicer = new OrderSlicingAlgorithm();
        
        OrderSlicingAlgorithm.SlicingConfig config = 
            new OrderSlicingAlgorithm.SlicingConfig(
                50000,  // 大单 50000
                0.03,
                500,
                5000,
                OrderSlicingAlgorithm.SlicingStrategy.ADAPTIVE,
                true,   // 随机化
                120     // 2 小时
            );
        
        // 高波动性市场
        OrderSlicingAlgorithm.MarketDepth highVolDepth = 
            new OrderSlicingAlgorithm.MarketDepth(10000, 10000, 0.05, 0.08);
        
        OrderSlicingAlgorithm.SlicingResult result = slicer.sliceOrder(config, highVolDepth);
        
        // 高波动时应该更快执行（切片数较少）
        assertTrue(result.getNumSlices() >= 5);
        assertTrue(result.getNumSlices() <= 50);
    }
    
    @Test
    @DisplayName("测试市场深度影响")
    void testMarketDepthImpact() {
        OrderSlicingAlgorithm slicer = new OrderSlicingAlgorithm();
        
        OrderSlicingAlgorithm.SlicingConfig config = 
            new OrderSlicingAlgorithm.SlicingConfig(
                20000,
                0.02,
                200,
                3000,
                OrderSlicingAlgorithm.SlicingStrategy.PROPORTIONAL,
                false,
                90
            );
        
        // 流动性好的市场
        OrderSlicingAlgorithm.MarketDepth goodLiquidity = 
            new OrderSlicingAlgorithm.MarketDepth(50000, 50000, 0.001, 0.01);
        
        // 流动性差的市场
        OrderSlicingAlgorithm.MarketDepth poorLiquidity = 
            new OrderSlicingAlgorithm.MarketDepth(1000, 1000, 0.05, 0.05);
        
        OrderSlicingAlgorithm.SlicingResult goodResult = slicer.sliceOrder(config, goodLiquidity);
        OrderSlicingAlgorithm.SlicingResult poorResult = slicer.sliceOrder(config, poorLiquidity);
        
        // 流动性好时可以更大的单次执行量
        assertTrue(goodResult.getAvgSliceSize() > poorResult.getAvgSliceSize());
    }
}
