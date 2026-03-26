package com.sor.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarketDataManager 单元测试
 */
@DisplayName("MarketDataManager 单元测试")
class MarketDataManagerTest {
    
    private MarketDataManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new MarketDataManager();
    }
    
    @Test
    @DisplayName("测试获取订单簿")
    void testGetOrderBook() {
        OrderBook book = manager.getOrderBook("AAPL");
        
        assertNotNull(book);
        assertEquals("AAPL", book.getSymbol());
    }
    
    @Test
    @DisplayName("测试更新买单")
    void testUpdateBid() {
        manager.updateBid("AAPL", 150.0, 100);
        manager.updateBid("AAPL", 149.5, 200);
        
        OrderBook book = manager.getOrderBook("AAPL");
        assertEquals(2, book.getBidCount());
        assertEquals(150.0, book.getBestBid());
    }
    
    @Test
    @DisplayName("测试更新卖单")
    void testUpdateAsk() {
        manager.updateAsk("AAPL", 151.0, 100);
        manager.updateAsk("AAPL", 150.5, 200);
        
        OrderBook book = manager.getOrderBook("AAPL");
        assertEquals(2, book.getAskCount());
        assertEquals(150.5, book.getBestAsk());
    }
    
    @Test
    @DisplayName("测试获取最优价格")
    void testGetBestPrices() {
        manager.updateBid("AAPL", 150.0, 100);
        manager.updateAsk("AAPL", 151.0, 100);
        
        assertEquals(150.0, manager.getBestBid("AAPL"));
        assertEquals(151.0, manager.getBestAsk("AAPL"));
    }
    
    @Test
    @DisplayName("测试清除订单簿")
    void testClearOrderBook() {
        manager.updateBid("AAPL", 150.0, 100);
        manager.updateAsk("AAPL", 151.0, 100);
        
        manager.clearOrderBook("AAPL");
        
        OrderBook book = manager.getOrderBook("AAPL");
        assertEquals(0, book.getBidCount());
        assertEquals(0, book.getAskCount());
    }
    
    @Test
    @DisplayName("测试多交易品种")
    void testMultipleSymbols() {
        manager.updateBid("AAPL", 150.0, 100);
        manager.updateBid("GOOG", 2800.0, 50);
        manager.updateBid("MSFT", 330.0, 200);
        
        assertEquals(150.0, manager.getBestBid("AAPL"));
        assertEquals(2800.0, manager.getBestBid("GOOG"));
        assertEquals(330.0, manager.getBestBid("MSFT"));
    }
}
