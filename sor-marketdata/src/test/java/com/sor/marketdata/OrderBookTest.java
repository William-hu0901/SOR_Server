package com.sor.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderBook 单元测试
 */
@DisplayName("OrderBook 单元测试")
class OrderBookTest {
    
    private OrderBook orderBook;
    
    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("AAPL", 10);
    }
    
    @Test
    @DisplayName("测试初始化")
    void testInitialization() {
        assertEquals("AAPL", orderBook.getSymbol());
        assertEquals(0, orderBook.getBidCount());
        assertEquals(0, orderBook.getAskCount());
        assertEquals(0.0, orderBook.getBestBid());
        assertEquals(0.0, orderBook.getBestAsk());
    }
    
    @Test
    @DisplayName("测试添加买单")
    void testAddBid() {
        orderBook.addBid(150.0, 100);
        orderBook.addBid(149.5, 200);
        orderBook.addBid(150.5, 150);  // 最高价
        
        assertEquals(3, orderBook.getBidCount());
        assertEquals(150.5, orderBook.getBestBid());  // 最高价在前
    }
    
    @Test
    @DisplayName("测试添加卖单")
    void testAddAsk() {
        orderBook.addAsk(151.0, 100);
        orderBook.addAsk(150.5, 200);  // 最低价
        orderBook.addAsk(151.5, 150);
        
        assertEquals(3, orderBook.getAskCount());
        assertEquals(150.5, orderBook.getBestAsk());  // 最低价在前
    }
    
    @Test
    @DisplayName("测试价格聚合")
    void testPriceAggregation() {
        orderBook.addBid(150.0, 100);
        orderBook.addBid(150.0, 200);  // 相同价格
        orderBook.addBid(150.0, 50);   // 相同价格
        
        assertEquals(1, orderBook.getBidCount());  // 应该聚合
        assertEquals(150.0, orderBook.getBestBid());
    }
    
    @Test
    @DisplayName("测试移除订单")
    void testRemoveOrder() {
        orderBook.addBid(150.0, 100);
        orderBook.addBid(149.0, 200);
        
        orderBook.removeBid(150.0);
        
        assertEquals(1, orderBook.getBidCount());
        assertEquals(149.0, orderBook.getBestBid());
    }
    
    @Test
    @DisplayName("测试价差计算")
    void testSpreadCalculation() {
        orderBook.addBid(150.0, 100);
        orderBook.addAsk(151.0, 100);
        
        assertEquals(1.0, orderBook.getSpread());
    }
    
    @Test
    @DisplayName("测试中间价计算")
    void testMidPriceCalculation() {
        orderBook.addBid(150.0, 100);
        orderBook.addAsk(152.0, 100);
        
        assertEquals(151.0, orderBook.getMidPrice());
    }
    
    @Test
    @DisplayName("测试清空订单簿")
    void testClear() {
        orderBook.addBid(150.0, 100);
        orderBook.addAsk(151.0, 100);
        
        orderBook.clear();
        
        assertEquals(0, orderBook.getBidCount());
        assertEquals(0, orderBook.getAskCount());
    }
    
    @Test
    @DisplayName("测试时间戳更新")
    void testTimestampUpdate() throws InterruptedException {
        long initialTimestamp = orderBook.getTimestamp();
        
        Thread.sleep(1);  // 确保时间流逝
        
        orderBook.addBid(150.0, 100);
        
        assertTrue(orderBook.getTimestamp() > initialTimestamp);
    }
}
