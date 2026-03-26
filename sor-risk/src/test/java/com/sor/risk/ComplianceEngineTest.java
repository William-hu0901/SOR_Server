package com.sor.risk;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ComplianceEngine 单元测试
 */
@DisplayName("ComplianceEngine 单元测试")
class ComplianceEngineTest {
    
    private ComplianceEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new ComplianceEngine();
    }
    
    @Test
    @DisplayName("测试无检查器时自动通过")
    void testNoCheckersAutoPass() {
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 150.0, System.nanoTime()
        );
        
        RiskChecker.RiskCheckResult result = engine.check(order);
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("测试添加检查器")
    void testAddChecker() {
        PriceLimitChecker checker = new PriceLimitChecker(10.0);
        engine.addChecker(checker);
        
        assertEquals(1, engine.getCheckerCount());
    }
    
    @Test
    @DisplayName("测试所有检查器通过")
    void testAllCheckersPass() {
        engine.addChecker(new PriceLimitChecker(10.0));
        engine.addChecker(new QuantityLimitChecker(1, 10000));
        
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 150.0, System.nanoTime()
        );
        
        RiskChecker.RiskCheckResult result = engine.check(order);
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("测试快速失败")
    void testFastFail() {
        PriceLimitChecker priceChecker = new PriceLimitChecker(10.0);
        priceChecker.setReferencePrice(150.0);  // 设置参考价格为 150
        engine.addChecker(priceChecker);
        engine.addChecker(new QuantityLimitChecker(1, 10000));
        
        Order order = new Order(
            1L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
            100, 200.0, System.nanoTime()  // 价格超标 (200 vs 150, +33%)
        );
        
        RiskChecker.RiskCheckResult result = engine.check(order);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("deviation"));
    }
}
