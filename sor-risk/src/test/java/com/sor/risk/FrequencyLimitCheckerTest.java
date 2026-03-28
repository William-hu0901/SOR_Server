package com.sor.risk;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FrequencyLimitChecker 单元测试
 */
@DisplayName("FrequencyLimitChecker 单元测试")
class FrequencyLimitCheckerTest {
    
    @Test
    @DisplayName("测试频率在限制内")
    void testFrequencyWithinLimit() {
        FrequencyLimitChecker checker = new FrequencyLimitChecker(10, 20, 50);
        
        for (int i = 0; i < 5; i++) {
            Order order = new Order(
                1000L + i, "AAPL", OrderSide.BUY, OrderType.LIMIT,
                100, 150.0, System.nanoTime()
            );
            
            RiskChecker.RiskCheckResult result = checker.check(order);
            assertTrue(result.isPassed());
        }
    }
    

    @Test
    @DisplayName("测试频率限制器正常工作")
    void testFrequencyLimiterWorks() {
        // 使用极低的限制
        FrequencyLimitChecker checker = new FrequencyLimitChecker(2, 5, 10);
        
        // 前两个订单应该通过（来自同一个交易者）
        Order order1 = new Order(1000L, "AAPL", OrderSide.BUY, OrderType.LIMIT, 100, 150.0, System.nanoTime());
        Order order2 = new Order(1000L, "AAPL", OrderSide.BUY, OrderType.LIMIT, 100, 150.0, System.nanoTime()); // 使用相同的订单ID
        
        assertTrue(checker.check(order1).isPassed());
        assertTrue(checker.check(order2).isPassed());
        
        // 第三个订单应该被 trader 限制拦截（来自同一个交易者）
        Order order3 = new Order(1000L, "AAPL", OrderSide.BUY, OrderType.LIMIT, 100, 150.0, System.nanoTime()); // 使用相同的订单ID
        RiskChecker.RiskCheckResult result3 = checker.check(order3);
        assertFalse(result3.isPassed());
        assertTrue(result3.getReason().contains("frequency"));
    }
}