package com.sor.risk;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QuantityLimitChecker 单元测试
 */
@DisplayName("QuantityLimitChecker 单元测试")
class QuantityLimitCheckerTest {
    
    private QuantityLimitChecker checker;
    
    @BeforeEach
    void setUp() {
        checker = new QuantityLimitChecker(1, 10000);  // 1-10000
    }
    
    @Test
    @DisplayName("测试数量在限制内")
    void testQuantityWithinLimit() {
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            5000, 150.0, System.nanoTime()
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("测试数量低于最小值")
    void testQuantityBelowMinimum() {
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            0, 150.0, System.nanoTime()  // < 1
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("below minimum"));
    }
    
    @Test
    @DisplayName("测试数量超过最大值")
    void testQuantityExceedsMaximum() {
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            15000, 150.0, System.nanoTime()  // > 10000
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("exceeds maximum"));
    }
}
