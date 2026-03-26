package com.sor.risk;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Risk 模块使用示例
 */
public class RiskExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(RiskExample.class);
    
    public static void main(String[] args) {
        // 获取风险管理器实例（单例）
        RiskManager riskManager = RiskManager.getInstance();
        
        // 创建测试订单
        Order order1 = new Order(
            1L,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            100,           // 数量
            155.0,         // 价格（超标，会被拒绝）
            System.nanoTime()
        );
        
        Order order2 = new Order(
            2L,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            100,           // 数量
            160.0,         // 价格（合理，会通过）
            System.nanoTime()
        );
        
        Order order3 = new Order(
            3L,
            "AAPL",
            OrderSide.BUY,
            OrderType.LIMIT,
            15000,         // 数量超标（会被拒绝）
            150.0,
            System.nanoTime()
        );
        
        LOG.info("=== Risk Check Examples ===");
        
        // 测试订单 1（价格超标）
        LOG.info("\nTesting Order 1 (price too high): {}", order1);
        boolean passed1 = riskManager.checkOrder(order1);
        LOG.info("Result: {}\n", passed1 ? "PASS" : "REJECT");
        
        // 更新参考价格后再测试
        LOG.info("Updating reference price to 150.0");
        riskManager.updateReferencePrice("AAPL", 150.0);
        
        // 测试订单 2（价格合理）
        LOG.info("\nTesting Order 2 (reasonable price): {}", order2);
        boolean passed2 = riskManager.checkOrder(order2);
        LOG.info("Result: {}\n", passed2 ? "PASS" : "REJECT");
        
        // 测试订单 3（数量超标）
        LOG.info("\nTesting Order 3 (quantity exceeded): {}", order3);
        boolean passed3 = riskManager.checkOrder(order3);
        LOG.info("Result: {}\n", passed3 ? "PASS" : "REJECT");
        
        // 打印统计报告
        LOG.info("\n=== Risk Statistics ===");
        riskManager.printStatistics();
        
        // 测试频率限制
        LOG.info("\n=== Testing Frequency Limit ===");
        testFrequencyLimit(riskManager);
        
        // 最终统计
        riskManager.printStatistics();
    }
    
    /**
     * 测试频率限制
     */
    private static void testFrequencyLimit(RiskManager riskManager) {
        LOG.info("Submitting 105 orders rapidly...");
        
        int passed = 0;
        int rejected = 0;
        
        for (int i = 0; i < 105; i++) {
            Order order = new Order(
                1000L + i,  // 同一交易者的订单
                "AAPL",
                OrderSide.BUY,
                OrderType.LIMIT,
                100,
                150.0,
                System.nanoTime()
            );
            
            if (riskManager.checkOrder(order)) {
                passed++;
            } else {
                rejected++;
            }
        }
        
        LOG.info("Frequency test result: {} passed, {} rejected", passed, rejected);
        LOG.info("(Trader limit is 100 orders/sec)");
    }
}
