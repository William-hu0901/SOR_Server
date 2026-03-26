package com.sor.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单匹配算法单元测试
 */
@DisplayName("订单匹配算法测试")
class OrderMatchingAlgorithmTest {
    
    /**
     * 测试订单实现
     */
    private static class TestOrder implements OrderMatchingAlgorithm.Order {
        private final long orderId;
        private final double price;
        private final long quantity;
        private final OrderMatchingAlgorithm.Order.Side side;
        private final long timestamp;
        
        public TestOrder(long orderId, double price, long quantity, 
                        OrderMatchingAlgorithm.Order.Side side, long timestamp) {
            this.orderId = orderId;
            this.price = price;
            this.quantity = quantity;
            this.side = side;
            this.timestamp = timestamp;
        }
        
        @Override
        public long getOrderId() { return orderId; }
        @Override
        public double getPrice() { return price; }
        @Override
        public long getQuantity() { return quantity; }
        @Override
        public OrderMatchingAlgorithm.Order.Side getSide() { return side; }
        @Override
        public long getTimestamp() { return timestamp; }
    }
    
    @Test
    @DisplayName("测试买单与卖单完全匹配")
    void testFullMatch() {
        OrderMatchingAlgorithm matcher = new OrderMatchingAlgorithm();
        
        // 创建卖单簿
        java.util.List<OrderMatchingAlgorithm.PriceLevel> sellBook = new java.util.ArrayList<>();
        OrderMatchingAlgorithm.PriceLevel level1 = new OrderMatchingAlgorithm.PriceLevel(100.0);
        level1.addOrder(new TestOrder(2L, 100.0, 500, 
            OrderMatchingAlgorithm.Order.Side.SELL, System.nanoTime()));
        sellBook.add(level1);
        
        // 创建买单（价格 100，数量 300）
        TestOrder buyOrder = new TestOrder(1L, 100.0, 300, 
            OrderMatchingAlgorithm.Order.Side.BUY, System.nanoTime());
        
        OrderMatchingAlgorithm.MatchResult result = matcher.matchOrder(buyOrder, sellBook);
        
        assertTrue(result.isFullyFilled());
        assertEquals(1, result.getTradeCount());
        assertEquals(0, result.getRemainingQuantity());
        
        OrderMatchingAlgorithm.Trade trade = result.getTrades().get(0);
        assertEquals(300, trade.getQuantity());
        assertEquals(100.0, trade.getPrice());
    }
    
    @Test
    @DisplayName("测试部分匹配")
    void testPartialMatch() {
        OrderMatchingAlgorithm matcher = new OrderMatchingAlgorithm();
        
        // 卖单簿只有 200 股
        java.util.List<OrderMatchingAlgorithm.PriceLevel> sellBook = new java.util.ArrayList<>();
        OrderMatchingAlgorithm.PriceLevel level1 = new OrderMatchingAlgorithm.PriceLevel(100.0);
        level1.addOrder(new TestOrder(2L, 100.0, 200, 
            OrderMatchingAlgorithm.Order.Side.SELL, System.nanoTime()));
        sellBook.add(level1);
        
        // 买单需要 500 股
        TestOrder buyOrder = new TestOrder(1L, 100.0, 500, 
            OrderMatchingAlgorithm.Order.Side.BUY, System.nanoTime());
        
        OrderMatchingAlgorithm.MatchResult result = matcher.matchOrder(buyOrder, sellBook);
        
        assertFalse(result.isFullyFilled());
        assertEquals(300, result.getRemainingQuantity());
        assertEquals(200, result.getTrades().get(0).getQuantity());
    }
    
    @Test
    @DisplayName("测试多价格水平匹配")
    void testMultiLevelMatch() {
        OrderMatchingAlgorithm matcher = new OrderMatchingAlgorithm();
        
        // 创建多个价格水平的卖单簿
        java.util.List<OrderMatchingAlgorithm.PriceLevel> sellBook = new java.util.ArrayList<>();
        
        OrderMatchingAlgorithm.PriceLevel level1 = new OrderMatchingAlgorithm.PriceLevel(99.5);
        level1.addOrder(new TestOrder(2L, 99.5, 100, 
            OrderMatchingAlgorithm.Order.Side.SELL, System.nanoTime()));
        
        OrderMatchingAlgorithm.PriceLevel level2 = new OrderMatchingAlgorithm.PriceLevel(100.0);
        level2.addOrder(new TestOrder(3L, 100.0, 200, 
            OrderMatchingAlgorithm.Order.Side.SELL, System.nanoTime()));
        
        sellBook.add(level1);
        sellBook.add(level2);
        
        // 买单价格为 100（可以吃掉两个价位）
        TestOrder buyOrder = new TestOrder(1L, 100.0, 250, 
            OrderMatchingAlgorithm.Order.Side.BUY, System.nanoTime());
        
        OrderMatchingAlgorithm.MatchResult result = matcher.matchOrder(buyOrder, sellBook);
        
        assertTrue(result.isFullyFilled());
        assertEquals(2, result.getTradeCount());
        
        // 第一笔：99.5 @ 100
        assertEquals(99.5, result.getTrades().get(0).getPrice());
        assertEquals(100, result.getTrades().get(0).getQuantity());
        
        // 第二笔：100.0 @ 150
        assertEquals(100.0, result.getTrades().get(1).getPrice());
        assertEquals(150, result.getTrades().get(1).getQuantity());
    }
}
