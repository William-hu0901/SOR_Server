package com.sor.core.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 流动性聚合算法（Liquidity Aggregation Algorithm）
 * 
 * 算法描述：
 * 流动性聚合是智能订单路由（SOR）的核心功能，它将多个交易所或流动性提供商的
 * 订单簿合并为一个统一的虚拟订单簿，从而实现最优订单执行。
 * 
 * 核心目标：
 * - 深度最大化：聚合所有可用流动性
 * - 价格优化：自动选择最优价格
 * - 延迟最小化：快速响应市场变化
 * - 成本控制：考虑交易费用和滑点
 * 
 * 聚合策略：
 * 1. 价格优先聚合：按价格优先级合并相同价格的流动性
 * 2. 加权聚合：根据交易所可靠性分配权重
 * 3. 分层聚合：区分不同等级的流动性提供商
 * 
 * 技术挑战：
 * - 多源数据同步（时钟对齐）
 * - 数据冲突解决（stale quote）
 * - 性能与准确性的平衡
 * - 网络延迟差异处理
 * 
 * 应用场景：
 * - 智能订单路由系统
 * - 多交易所套利
 * - 流动性热点分析
 * - 最佳执行报告生成
 * 
 * @author SOR Team
 * @version 1.0
 */
public class LiquidityAggregationAlgorithm {
    
    /**
     * 流动性提供商接口
     */
    public interface LiquidityProvider {
        String getProviderId();
        double getMakerFee();      // 挂单费率
        double getTakerFee();      // 吃单费率
        double getLatency();       // 延迟（毫秒）
        double getReliability();   // 可靠性评分 (0-1)
    }
    
    /**
     * 订单项 - 来自特定流动性提供商
     */
    public static class OrderItem implements Comparable<OrderItem> {
        private final String providerId;
        private final double price;
        private final long quantity;
        private final Side side;
        private final long timestamp;
        
        public OrderItem(String providerId, double price, long quantity, Side side, long timestamp) {
            this.providerId = providerId;
            this.price = price;
            this.quantity = quantity;
            this.side = side;
            this.timestamp = timestamp;
        }
        
        public enum Side { BID, ASK }
        
        public String getProviderId() { return providerId; }
        public double getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public Side getSide() { return side; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public int compareTo(OrderItem other) {
            if (this.side != other.side) {
                throw new IllegalArgumentException("Cannot compare orders with different sides");
            }
            
            // 买单：价格高的在前；卖单：价格低的在前
            int priceCompare = Double.compare(other.price, this.price);
            if (priceCompare != 0) {
                return priceCompare;
            }
            
            // 价格相同时，时间早的在前
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
    
    /**
     * 聚合价格水平
     */
    public static class AggregatedLevel {
        private final double price;
        private long totalQuantity;
        private final java.util.Map<String, Long> providerQuantities;
        
        public AggregatedLevel(double price) {
            this.price = price;
            this.totalQuantity = 0;
            this.providerQuantities = new ConcurrentHashMap<>();
        }
        
        public void addQuantity(String providerId, long quantity) {
            providerQuantities.merge(providerId, quantity, Long::sum);
            totalQuantity += quantity;
        }
        
        public double getPrice() { return price; }
        public long getTotalQuantity() { return totalQuantity; }
        public java.util.Map<String, Long> getProviderQuantities() {
            return new ConcurrentHashMap<>(providerQuantities);
        }
        
        /**
         * 获取加权平均延迟
         */
        public double getWeightedLatency(java.util.Map<String, Double> providerLatencies) {
            double totalWeight = 0;
            double weightedSum = 0;
            
            for (java.util.Map.Entry<String, Long> entry : providerQuantities.entrySet()) {
                double latency = providerLatencies.getOrDefault(entry.getKey(), 0.0);
                double weight = entry.getValue();
                weightedSum += latency * weight;
                totalWeight += weight;
            }
            
            return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        }
    }
    
    /**
     * 聚合订单簿
     */
    public static class AggregatedBook {
        private final java.util.NavigableMap<Double, AggregatedLevel> bids;  // 买盘（降序）
        private final java.util.NavigableMap<Double, AggregatedLevel> asks;  // 卖盘（升序）
        private final long timestamp;
        
        public AggregatedBook(long timestamp) {
            // 使用 TreeMap 自动排序
            this.bids = new java.util.TreeMap<>(Double::compare);
            this.asks = new java.util.TreeMap<>(Double::compare);
            this.timestamp = timestamp;
        }
        
        public void addBid(double price, long quantity, String providerId) {
            bids.computeIfAbsent(price, k -> new AggregatedLevel(price))
                .addQuantity(providerId, quantity);
        }
        
        public void addAsk(double price, long quantity, String providerId) {
            asks.computeIfAbsent(price, k -> new AggregatedLevel(price))
                .addQuantity(providerId, quantity);
        }
        
        public java.util.NavigableMap<Double, AggregatedLevel> getBids() {
            // 返回降序视图（价格从高到低）
            return bids.descendingMap();
        }
        
        public java.util.NavigableMap<Double, AggregatedLevel> getAsks() {
            return asks;  // 已经是升序（价格从低到高）
        }
        
        public double getBestBid() {
            return bids.isEmpty() ? 0.0 : bids.lastKey();
        }
        
        public double getBestAsk() {
            return asks.isEmpty() ? 0.0 : asks.firstKey();
        }
        
        public double getSpread() {
            if (bids.isEmpty() || asks.isEmpty()) {
                return 0.0;
            }
            return getBestAsk() - getBestBid();
        }
        
        public double getMidPrice() {
            if (bids.isEmpty() || asks.isEmpty()) {
                return 0.0;
            }
            return (getBestBid() + getBestAsk()) / 2.0;
        }
        
        public long getTimestamp() { return timestamp; }
        
        /**
         * 获取前 N 档买卖盘
         */
        public java.util.List<AggregatedLevel> getTopBids(int n) {
            return getBids().values().stream()
                .limit(n)
                .collect(Collectors.toList());
        }
        
        public java.util.List<AggregatedLevel> getTopAsks(int n) {
            return getAsks().values().stream()
                .limit(n)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 主聚合函数 - 将多个订单簿聚合成一个虚拟订单簿
     * 
     * @param allOrders 所有订单（来自不同流动性提供商）
     * @return 聚合后的订单簿
     */
    public AggregatedBook aggregate(java.util.List<OrderItem> allOrders) {
        long aggregationTime = System.nanoTime();
        AggregatedBook book = new AggregatedBook(aggregationTime);
        
        // 遍历所有订单并聚合
        for (OrderItem order : allOrders) {
            if (order.getSide() == OrderItem.Side.BID) {
                book.addBid(order.getPrice(), order.getQuantity(), order.getProviderId());
            } else {
                book.addAsk(order.getPrice(), order.getQuantity(), order.getProviderId());
            }
        }
        
        return book;
    }
    
    /**
     * 智能路由 - 为给定订单找到最优执行路径
     * 
     * @param side 订单方向
     * @param quantity 订单数量
     * @param book 聚合订单簿
     * @param providers 流动性提供商信息
     * @return 执行路径（按优先级排序的提供商列表）
     */
    public java.util.List<ExecutionPath> findOptimalPath(
            OrderItem.Side side,
            long quantity,
            AggregatedBook book,
            java.util.List<LiquidityProvider> providers) {
        
        java.util.List<ExecutionPath> paths = new java.util.ArrayList<>();
        
        // 选择合适的订单簿
        java.util.NavigableMap<Double, AggregatedLevel> levels = 
            (side == OrderItem.Side.BID) ? book.getAsks() : book.getBids();
        
        long remainingQty = quantity;
        double cumulativeValue = 0.0;
        
        // 按价格优先级遍历
        for (AggregatedLevel level : levels.values()) {
            if (remainingQty <= 0) {
                break;
            }
            
            // 计算该价格水平可执行的量
            long execQty = Math.min(remainingQty, level.getTotalQuantity());
            
            // 在提供商间分配（考虑费用和延迟）
            java.util.Map<String, Long> allocation = allocateAmongProviders(
                execQty, 
                level.getProviderQuantities(),
                providers
            );
            
            // 创建执行路径
            for (java.util.Map.Entry<String, Long> entry : allocation.entrySet()) {
                if (entry.getValue() > 0) {
                    paths.add(new ExecutionPath(
                        entry.getKey(),
                        level.getPrice(),
                        entry.getValue(),
                        side
                    ));
                }
            }
            
            cumulativeValue += level.getPrice() * execQty;
            remainingQty -= execQty;
        }
        
        return paths;
    }
    
    /**
     * 在多个流动性提供商间分配订单
     * 
     * @param totalQuantity 总数量
     * @param providerQuantities 各提供商可用数量
     * @param providers 提供商信息
     * @return 分配方案
     */
    private java.util.Map<String, Long> allocateAmongProviders(
            long totalQuantity,
            java.util.Map<String, Long> providerQuantities,
            java.util.List<LiquidityProvider> providers) {
        
        java.util.Map<String, Long> allocation = new ConcurrentHashMap<>();
        long remainingQty = totalQuantity;
        
        // 创建提供商映射
        java.util.Map<String, LiquidityProvider> providerMap = new ConcurrentHashMap<>();
        for (LiquidityProvider lp : providers) {
            providerMap.put(lp.getProviderId(), lp);
        }
        
        // 按综合成本排序（费用 + 延迟）
        java.util.List<String> sortedProviders = providerQuantities.keySet().stream()
            .sorted((id1, id2) -> {
                LiquidityProvider lp1 = providerMap.get(id1);
                LiquidityProvider lp2 = providerMap.get(id2);
                
                if (lp1 == null || lp2 == null) {
                    return 0;
                }
                
                // 综合成本 = 费用 + 延迟惩罚
                double cost1 = lp1.getTakerFee() + (lp1.getLatency() * 0.001);
                double cost2 = lp2.getTakerFee() + (lp2.getLatency() * 0.001);
                
                return Double.compare(cost1, cost2);
            })
            .collect(Collectors.toList());
        
        // 依次分配
        for (String providerId : sortedProviders) {
            if (remainingQty <= 0) {
                break;
            }
            
            long availableQty = providerQuantities.getOrDefault(providerId, 0L);
            long allocQty = Math.min(availableQty, remainingQty);
            
            allocation.put(providerId, allocQty);
            remainingQty -= allocQty;
        }
        
        return allocation;
    }
    
    /**
     * 执行路径 - 描述在某个提供商处的执行计划
     */
    public static class ExecutionPath {
        private final String providerId;
        private final double price;
        private final long quantity;
        private final OrderItem.Side side;
        private final double estimatedCost;
        
        public ExecutionPath(String providerId, double price, long quantity, OrderItem.Side side) {
            this.providerId = providerId;
            this.price = price;
            this.quantity = quantity;
            this.side = side;
            this.estimatedCost = price * quantity;
        }
        
        public String getProviderId() { return providerId; }
        public double getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public OrderItem.Side getSide() { return side; }
        public double getEstimatedCost() { return estimatedCost; }
        
        @Override
        public String toString() {
            return String.format("Path{%s: %d@%.2f = %.2f}", 
                providerId, quantity, price, estimatedCost);
        }
    }
}
