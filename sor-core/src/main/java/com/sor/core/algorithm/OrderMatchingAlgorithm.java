package com.sor.core.algorithm;

/**
 * 订单匹配算法（Order Matching Algorithm）
 * 
 * 算法描述：
 * 实现经典的限价订单簿匹配逻辑，遵循"价格优先、时间优先"原则。
 * 该算法是交易所和 ECN（电子通信网络）的核心组件，用于撮合买单和卖单。
 * 
 * 核心规则：
 * - 价格优先：较高买价优先于较低买价，较低卖价优先于较高卖价
 * - 时间优先：同一价格的订单，先到的订单优先成交
 * - 数量匹配：成交价格取订单中较早一方的价格，成交量取两者较小值
 * 
 * 应用场景：
 * - 证券交易所订单撮合
 * - 加密货币交易所
 * - 外汇 ECN 平台
 * - 智能订单路由中的流动性评估
 * 
 * 性能特征：
 * - 时间复杂度：O(n)，n 为对手方订单簿深度
 * - 空间复杂度：O(1)，仅需要常量额外空间
 * - 适用于低延迟场景，单次匹配通常在微秒级别
 * 
 * @author SOR Team
 * @version 1.0
 */
public class OrderMatchingAlgorithm {
    
    /**
     * 订单接口 - 定义订单的基本属性
     */
    public interface Order {
        long getOrderId();
        double getPrice();
        long getQuantity();
        Side getSide();
        long getTimestamp();
        
        enum Side {
            BUY, SELL
        }
    }
    
    // Package-private constructor to prevent direct instantiation
    OrderMatchingAlgorithm() {}
    
    /**
     * 成交记录 - 描述一次匹配的结果
     */
    public static class Trade {
        private final long buyOrderId;
        private final long sellOrderId;
        private final double price;
        private final long quantity;
        private final long timestamp;
        
        public Trade(long buyOrderId, long sellOrderId, double price, long quantity) {
            this.buyOrderId = buyOrderId;
            this.sellOrderId = sellOrderId;
            this.price = price;
            this.quantity = quantity;
            this.timestamp = System.nanoTime();
        }
        
        public long getBuyOrderId() { return buyOrderId; }
        public long getSellOrderId() { return sellOrderId; }
        public double getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("Trade{price=%.2f, qty=%d, buy=%d, sell=%d}", 
                price, quantity, buyOrderId, sellOrderId);
        }
    }
    
    /**
     * 匹配结果 - 包含所有成交记录和剩余订单
     */
    public static class MatchResult {
        private final java.util.List<Trade> trades;
        private final long remainingQuantity;
        
        public MatchResult(java.util.List<Trade> trades, long remainingQuantity) {
            this.trades = trades;
            this.remainingQuantity = remainingQuantity;
        }
        
        public java.util.List<Trade> getTrades() { return trades; }
        public long getRemainingQuantity() { return remainingQuantity; }
        public boolean isFullyFilled() { return remainingQuantity == 0; }
        public int getTradeCount() { return trades.size(); }
    }
    
    /**
     * 价格水平 - 聚合同一价格的所有订单
     */
    public static class PriceLevel implements Comparable<PriceLevel> {
        private final double price;
        private long totalQuantity;
        private final java.util.List<Order> orders;
        
        public PriceLevel(double price) {
            this.price = price;
            this.totalQuantity = 0;
            this.orders = new java.util.ArrayList<>();
        }
        
        public void addOrder(Order order) {
            orders.add(order);
            totalQuantity += order.getQuantity();
        }
        
        public double getPrice() { return price; }
        public long getTotalQuantity() { return totalQuantity; }
        public java.util.List<Order> getOrders() { return orders; }
        
        @Override
        public int compareTo(PriceLevel other) {
            // 买盘：价格高的在前；卖盘：价格低的在前（由调用方决定排序方向）
            return Double.compare(other.price, this.price);
        }
    }
    
    /**
     * 主匹配函数 - 将新订单与对手方订单簿进行匹配
     * 
     * @param incomingOrder 新进入的订单
     * @param oppositeBook 对手方订单簿（如果是买单则为卖单簿，反之亦然）
     * @return 匹配结果，包含所有成交和剩余数量
     */
    public MatchResult matchOrder(Order incomingOrder, java.util.List<PriceLevel> oppositeBook) {
        java.util.List<Trade> trades = new java.util.ArrayList<>();
        long remainingQty = incomingOrder.getQuantity();
        
        // 遍历对手方订单簿（已按价格优先级排序）
        for (PriceLevel level : oppositeBook) {
            if (remainingQty <= 0) {
                break;
            }
            
            // 检查价格是否可成交
            if (!canMatch(incomingOrder, level.getPrice())) {
                continue;
            }
            
            // 在当前价格水平上匹配订单
            for (Order oppositeOrder : level.getOrders()) {
                if (remainingQty <= 0) {
                    break;
                }
                
                // 计算成交量（取两者较小值）
                long tradeQty = Math.min(remainingQty, oppositeOrder.getQuantity());
                
                // 确定成交价格（取订单中较早一方的价格）
                double tradePrice = determineTradePrice(incomingOrder, oppositeOrder);
                
                // 创建成交记录
                Trade trade = createTrade(incomingOrder, oppositeOrder, tradePrice, tradeQty);
                trades.add(trade);
                
                // 更新剩余数量
                remainingQty -= tradeQty;
            }
        }
        
        return new MatchResult(trades, remainingQty);
    }
    
    /**
     * 判断订单是否可以与给定价格匹配
     * 
     * <p>核心逻辑：</p>
     * <ul>
     *     <li>买单：买入价 >= 卖价 则可以成交</li>
     *     <li>卖单：卖出价 <= 买价 则可以成交</li>
     * </ul>
     * 
     * @param order 订单
     * @param oppositePrice 对手方价格
     * @return 是否可以匹配
     */
    private boolean canMatch(Order order, double oppositePrice) {
        if (order.getSide() == Order.Side.BUY) {
            // 买单：限价必须大于等于对手方卖价
            return order.getPrice() >= oppositePrice;
        } else {
            // 卖单：限价必须小于等于对手方买价
            return order.getPrice() <= oppositePrice;
        }
    }
    
    /**
     * 确定成交价格
     * 
     * <p>遵循国际惯例：价格优先于时间，成交价取被动方价格</p>
     * <ul>
     *     <li>如果买单先到：成交价 = 买单价格</li>
     *     <li>如果卖单先到：成交价 = 卖单价格</li>
     * </ul>
     * 
     * @param order1 订单 1
     * @param order2 订单 2
     * @return 成交价格
     */
    private double determineTradePrice(Order order1, Order order2) {
        // 比较时间戳，取较早的订单价格
        if (order1.getTimestamp() <= order2.getTimestamp()) {
            return order1.getPrice();
        } else {
            return order2.getPrice();
        }
    }
    
    /**
     * 创建成交记录
     * 
     * @param order1 订单 1
     * @param order2 订单 2
     * @param price 成交价格
     * @param quantity 成交数量
     * @return 成交记录
     */
    private Trade createTrade(Order order1, Order order2, double price, long quantity) {
        // 确定哪一个是买单，哪一个是卖单
        if (order1.getSide() == Order.Side.BUY) {
            return new Trade(order1.getOrderId(), order2.getOrderId(), price, quantity);
        } else {
            return new Trade(order2.getOrderId(), order1.getOrderId(), price, quantity);
        }
    }
    
    /**
     * 批量匹配 - 同时处理多个订单
     * 
     * @param orders 订单列表
     * @param buyBook 买单簿
     * @param sellBook 卖单簿
     * @return 所有成交记录
     */
    public java.util.List<Trade> batchMatch(
            java.util.List<Order> orders,
            java.util.List<PriceLevel> buyBook,
            java.util.List<PriceLevel> sellBook) {
        
        java.util.List<Trade> allTrades = new java.util.ArrayList<>();
        
        for (Order order : orders) {
            java.util.List<PriceLevel> oppositeBook = 
                (order.getSide() == Order.Side.BUY) ? sellBook : buyBook;
            
            MatchResult result = matchOrder(order, oppositeBook);
            allTrades.addAll(result.getTrades());
        }
        
        return allTrades;
    }
}
