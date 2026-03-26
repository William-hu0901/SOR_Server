package com.sor.risk;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceLimitChecker 单元测试
 */
@DisplayName("PriceLimitChecker 单元测试")
class PriceLimitCheckerTest {
    
    private PriceLimitChecker checker;
    private Order order;
    
    @BeforeEach
    void setUp() {
        checker = new PriceLimitChecker(10.0);  // 10% 限制
        checker.setReferencePrice(150.0);
        
        order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 160.0, System.nanoTime()
        );
    }
    
    @Test
    @DisplayName("测试价格在限制内")
    void testPriceWithinLimit() {
        order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 160.0, System.nanoTime()  // +6.67%
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("测试价格超出限制")
    void testPriceExceedsLimit() {
        order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 170.0, System.nanoTime()  // +13.33% > 10%
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("deviation"));
    }
    
    @Test
    @DisplayName("测试市价单跳过检查")
    void testMarketOrderSkipped() {
        order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.MARKET,
            100, 0.0, System.nanoTime()  // 市价单价格为 0
        );
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("测试无参考价格时跳过")
    void testNoReferencePrice() {
        checker = new PriceLimitChecker(10.0);
        // 未设置参考价格
        
        RiskChecker.RiskCheckResult result = checker.check(order);
        assertTrue(result.isPassed());
    }
}
