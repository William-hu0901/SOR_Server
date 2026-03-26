package com.sor.core.algorithm;

/**
 * VWAP 成交量加权平均价格算法（Volume Weighted Average Price）
 * 
 * 算法描述：
 * VWAP 是一种交易基准，表示一段时间内的平均成交价格，按成交量加权。
 * 它被广泛用于衡量交易执行质量，也是机构投资者常用的业绩评估指标。
 * 
 * 计算公式：
 * VWAP = Σ(Price × Volume) / Σ(Volume)
 * 即：VWAP = (P1×V1 + P2×V2 + ... + Pn×Vn) / (V1 + V2 + ... + Vn)
 * 
 * 核心特性：
 * - 滞后性：基于历史成交数据计算
 * - 代表性：反映市场真实成交成本
 * - 防操纵：大单对 VWAP 影响更大
 * 
 * 应用场景：
 * - 机构大额订单执行策略（VWAP Order）
 * - 交易员绩效评估
 * - 智能订单路由的基准价格
 * - 算法交易的执行目标
 * 
 * 变体：
 * - 锚定 VWAP：从特定时间点开始计算
 * - 滚动 VWAP：只计算最近 N 笔交易
 * - 预测 VWAP：结合订单簿预测未来 VWAP
 * 
 * 性能优化：
 * - 使用 long 类型避免浮点误差累积
 * - 增量更新而非全量重算
 * - 预分配容量减少对象创建
 * 
 * @author SOR Team
 * @version 1.0
 */
public class VWAPAlgorithm {
    
    /**
     * 成交记录 - 用于 VWAP 计算的基本单元
     */
    public static class Trade {
        private final double price;
        private final long volume;
        private final long timestamp;
        
        public Trade(double price, long volume, long timestamp) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        
        public double getPrice() { return price; }
        public long getVolume() { return volume; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * VWAP 计算结果
     */
    public static class VWAPResult {
        private final double vwap;
        private final long totalVolume;
        private final double totalValue;
        private final int tradeCount;
        private final long startTime;
        private final long endTime;
        
        public VWAPResult(double vwap, long totalVolume, double totalValue, 
                         int tradeCount, long startTime, long endTime) {
            this.vwap = vwap;
            this.totalVolume = totalVolume;
            this.totalValue = totalValue;
            this.tradeCount = tradeCount;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public double getVWAP() { return vwap; }
        public long getTotalVolume() { return totalVolume; }
        public double getTotalValue() { return totalValue; }
        public int getTradeCount() { return tradeCount; }
        public long getDuration() { return endTime - startTime; }
        
        @Override
        public String toString() {
            return String.format("VWAP{vwap=%.4f, volume=%d, trades=%d, duration=%.3fs}", 
                vwap, totalVolume, tradeCount, getDuration() / 1e9);
        }
    }
    
    /**
     * 增量 VWAP 计算器 - 支持流式数据
     */
    public static class IncrementalVWAPCalculator {
        private double cumulativePV = 0.0;  // 价格×成交量的累积值
        private long cumulativeVolume = 0;   // 累积成交量
        private int tradeCount = 0;
        private long firstTradeTime = Long.MAX_VALUE;
        private long lastTradeTime = Long.MIN_VALUE;
        
        /**
         * 添加新的成交记录
         * 
         * @param price 成交价格
         * @param volume 成交数量
         * @param timestamp 时间戳
         */
        public void addTrade(double price, long volume, long timestamp) {
            cumulativePV += price * volume;
            cumulativeVolume += volume;
            tradeCount++;
            
            if (timestamp < firstTradeTime) {
                firstTradeTime = timestamp;
            }
            if (timestamp > lastTradeTime) {
                lastTradeTime = timestamp;
            }
        }
        
        /**
         * 获取当前 VWAP 值
         * 
         * @return VWAP 值，如果没有成交则返回 0
         */
        public double getVWAP() {
            if (cumulativeVolume == 0) {
                return 0.0;
            }
            return cumulativePV / cumulativeVolume;
        }
        
        /**
         * 重置计算器
         */
        public void reset() {
            cumulativePV = 0.0;
            cumulativeVolume = 0;
            tradeCount = 0;
            firstTradeTime = Long.MAX_VALUE;
            lastTradeTime = Long.MIN_VALUE;
        }
        
        /**
         * 获取完整统计结果
         * 
         * @return VWAP 计算结果
         */
        public VWAPResult getResult() {
            double vwap = getVWAP();
            return new VWAPResult(
                vwap,
                cumulativeVolume,
                cumulativePV,
                tradeCount,
                firstTradeTime == Long.MAX_VALUE ? 0 : firstTradeTime,
                lastTradeTime == Long.MIN_VALUE ? 0 : lastTradeTime
            );
        }
    }
    
    /**
     * 批量计算 VWAP - 适用于历史数据分析
     * 
     * @param trades 成交记录列表
     * @return VWAP 计算结果
     */
    public VWAPResult calculate(java.util.List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return new VWAPResult(0.0, 0, 0.0, 0, 0, 0);
        }
        
        double totalPV = 0.0;
        long totalVolume = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        // 单次遍历计算所有统计量
        for (Trade trade : trades) {
            totalPV += trade.getPrice() * trade.getVolume();
            totalVolume += trade.getVolume();
            
            if (trade.getTimestamp() < minTime) {
                minTime = trade.getTimestamp();
            }
            if (trade.getTimestamp() > maxTime) {
                maxTime = trade.getTimestamp();
            }
        }
        
        double vwap = (totalVolume > 0) ? totalPV / totalVolume : 0.0;
        
        return new VWAPResult(vwap, totalVolume, totalPV, trades.size(), minTime, maxTime);
    }
    
    /**
     * 滑动窗口 VWAP - 只计算最近 N 笔交易
     * 
     * @param trades 成交记录列表（按时间正序排列）
     * @param windowSize 窗口大小（交易笔数）
     * @return VWAP 计算结果
     */
    public VWAPResult calculateSlidingWindow(java.util.List<Trade> trades, int windowSize) {
        if (trades == null || trades.isEmpty() || windowSize <= 0) {
            return new VWAPResult(0.0, 0, 0.0, 0, 0, 0);
        }
        
        // 确定窗口起始位置
        int startIndex = Math.max(0, trades.size() - windowSize);
        java.util.List<Trade> windowTrades = trades.subList(startIndex, trades.size());
        
        return calculate(windowTrades);
    }
    
    /**
     * 时间范围 VWAP - 计算指定时间段内的 VWAP
     * 
     * @param trades 成交记录列表
     * @param startTime 开始时间（纳秒）
     * @param endTime 结束时间（纳秒）
     * @return VWAP 计算结果
     */
    public VWAPResult calculateTimeRange(java.util.List<Trade> trades, 
                                         long startTime, long endTime) {
        if (trades == null || trades.isEmpty()) {
            return new VWAPResult(0.0, 0, 0.0, 0, 0, 0);
        }
        
        java.util.List<Trade> filteredTrades = new java.util.ArrayList<>();
        
        for (Trade trade : trades) {
            if (trade.getTimestamp() >= startTime && trade.getTimestamp() <= endTime) {
                filteredTrades.add(trade);
            }
        }
        
        return calculate(filteredTrades);
    }
    
    /**
     * 计算 VWAP 偏离度 - 衡量当前价格与 VWAP 的偏离程度
     * 
     * @param currentPrice 当前市场价格
     * @param vwap VWAP 值
     * @return 偏离度百分比（正数表示高于 VWAP）
     */
    public double calculateDeviation(double currentPrice, double vwap) {
        if (vwap == 0.0) {
            return 0.0;
        }
        return ((currentPrice - vwap) / vwap) * 100.0;
    }
    
    /**
     * 执行质量评估 - 比较实际成交均价与 VWAP
     * 
     * @param executionPrice 实际成交均价
     * @param vwap 同期 VWAP
     * @param isBuy 是否为买入交易
     * @return 执行质量评分（正数表示优于 VWAP）
     */
    public double evaluateExecutionQuality(double executionPrice, double vwap, boolean isBuy) {
        if (vwap == 0.0) {
            return 0.0;
        }
        
        double deviation = ((executionPrice - vwap) / vwap) * 100.0;
        
        // 买入时低于 VWAP 为好，卖出时高于 VWAP 为好
        return isBuy ? -deviation : deviation;
    }
}
