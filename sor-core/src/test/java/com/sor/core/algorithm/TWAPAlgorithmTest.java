package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TWAP 算法单元测试
 */
@DisplayName("TWAP 算法测试")
class TWAPAlgorithmTest {
    
    @Test
    @DisplayName("测试 TWAP 配置")
    void testTWAPConfig() {
        TWAPAlgorithm.TWAPConfig config = new TWAPAlgorithm.TWAPConfig(
            10000,  // 总数量
            10,     // 分 10 份
            60_000_000_000L  // 每分钟执行一次
        );
        
        assertEquals(1000, config.getSliceQuantity());
        assertEquals(10, config.getNumSlices());
        assertEquals(600_000_000_000L, config.getTotalDurationMillis() * 1_000_000);
    }
    
    @Test
    @DisplayName("测试 TWAP 调度器生成计划")
    void testTWAPScheduler() {
        TWAPAlgorithm.TWAPConfig config = new TWAPAlgorithm.TWAPConfig(
            5000, 5, 1_000_000_000L, 0.02, false
        );
        
        TWAPAlgorithm.TWAPScheduler scheduler = new TWAPAlgorithm.TWAPScheduler(config);
        long startTime = System.nanoTime();
        
        var slices = scheduler.generateSchedule(startTime);
        
        assertEquals(5, slices.size());
        
        // 验证时间间隔
        for (int i = 1; i < slices.size(); i++) {
            long expectedTime = startTime + i * 1_000_000_000L;
            assertEquals(expectedTime, slices.get(i).getScheduledTime(), 1_000_000);
        }
    }
    
    @Test
    @DisplayName("测试 TWAP 模拟执行")
    void testTWAPExecution() {
        TWAPAlgorithm.TWAPConfig config = new TWAPAlgorithm.TWAPConfig(
            1000, 5, 100_000_000L, 0.05, false
        );
        
        TWAPAlgorithm twap = new TWAPAlgorithm();
        
        // 模拟价格提供者（价格在 100 附近波动）
        double[] prices = {100.0, 100.5, 99.5, 101.0, 99.0};
        final int[] index = {0};
        
        TWAPAlgorithm.TWAPResult result = twap.execute(config, () -> {
            return prices[index[0]++];
        });
        
        assertEquals(5, result.getTotalSlices());
        assertEquals(5, result.getExecutedSlices());
        assertEquals(100.0, result.getTWAP(), 0.5);  // 平均价应该在 100 左右
    }
}
