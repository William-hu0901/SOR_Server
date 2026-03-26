package com.sor.core.algorithm;

/**
 * 订单分割算法（Order Slicing Algorithm）
 * 
 * 算法描述：
 * 订单分割（又称订单拆分）是将大额订单分解为多个小额订单的策略，
 * 目的是减少市场冲击成本、隐藏交易意图、优化执行价格。
 * 
 * 核心目标：
 * - 最小化冲击成本：避免大单对价格的负面影响
 * - 降低信息泄露：防止其他交易者察觉并抢先交易
 * - 优化执行价格：利用市场流动性波动
 * - 控制执行风险：平衡执行速度与价格风险
 * 
 * 分割策略：
 * 1. 等分策略：将订单均匀分成 N 份
 * 2. 比例策略：根据市场流动性按比例分配
 * 3. 随机策略：随机化每份的大小和时间
 * 4. 自适应策略：根据市场动态调整分割方案
 * 
 * 数学模型：
 * 最优分割数 N* = sqrt(总数量 / 市场深度)
 * 单次执行量 = 总数量 / N*
 * 冲击成本 ∝ (执行量 / 市场深度)^2
 * 
 * 应用场景：
 * - 机构大额股票交易
 * - 债券批量买卖
 * - 外汇大额兑换
 * - 加密货币大宗交易
 * 
 * @author SOR Team
 * @version 1.0
 */
public class OrderSlicingAlgorithm {
    
    /**
     * 订单分割配置
     */
    public static class SlicingConfig {
        private final long totalQuantity;           // 总订单数量
        private final double maxMarketImpact;       // 最大市场冲击（百分比）
        private final long minSliceQuantity;        // 最小切片数量
        private final long maxSliceQuantity;        // 最大切片数量
        private final SlicingStrategy strategy;     // 分割策略
        private final boolean randomizeSize;        // 是否随机化大小
        private final int timeHorizonMinutes;       // 执行时间窗口（分钟）
        
        public SlicingConfig(long totalQuantity, double maxMarketImpact,
                            long minSliceQty, long maxSliceQty) {
            this(totalQuantity, maxMarketImpact, minSliceQty, maxSliceQty,
                SlicingStrategy.ADAPTIVE, false, 60);
        }
        
        public SlicingConfig(long totalQuantity, double maxMarketImpact,
                            long minSliceQty, long maxSliceQty,
                            SlicingStrategy strategy, boolean randomizeSize,
                            int timeHorizonMinutes) {
            this.totalQuantity = totalQuantity;
            this.maxMarketImpact = maxMarketImpact;
            this.minSliceQuantity = minSliceQty;
            this.maxSliceQuantity = maxSliceQty;
            this.strategy = strategy;
            this.randomizeSize = randomizeSize;
            this.timeHorizonMinutes = timeHorizonMinutes;
        }
        
        public long getTotalQuantity() { return totalQuantity; }
        public double getMaxMarketImpact() { return maxMarketImpact; }
        public long getMinSliceQuantity() { return minSliceQuantity; }
        public long getMaxSliceQuantity() { return maxSliceQuantity; }
        public SlicingStrategy getStrategy() { return strategy; }
        public boolean isRandomizeSize() { return randomizeSize; }
        public int getTimeHorizonMinutes() { return timeHorizonMinutes; }
    }
    
    /**
     * 分割策略枚举
     */
    public enum SlicingStrategy {
        /** 等分策略 */
        EQUAL,
        /** 根据流动性比例分割 */
        PROPORTIONAL,
        /** 完全随机分割 */
        RANDOM,
        /** 自适应动态调整 */
        ADAPTIVE
    }
    
    /**
     * 订单切片 - 分割后的子订单
     */
    public static class OrderSlice {
        private final int sliceIndex;
        private final long quantity;
        private final double estimatedPrice;
        private final long scheduledTime;
        private Side side;
        private String symbol;
        
        public OrderSlice(int sliceIndex, long quantity, double estimatedPrice, long scheduledTime) {
            this.sliceIndex = sliceIndex;
            this.quantity = quantity;
            this.estimatedPrice = estimatedPrice;
            this.scheduledTime = scheduledTime;
        }
        
        public enum Side { BUY, SELL }
        
        public int getSliceIndex() { return sliceIndex; }
        public long getQuantity() { return quantity; }
        public double getEstimatedPrice() { return estimatedPrice; }
        public long getScheduledTime() { return scheduledTime; }
        public Side getSide() { return side; }
        public String getSymbol() { return symbol; }
        
        public void setSide(Side side) { this.side = side; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        
        @Override
        public String toString() {
            return String.format("Slice[%d]: %d @ %.2f", sliceIndex, quantity, estimatedPrice);
        }
    }
    
    /**
     * 分割结果
     */
    public static class SlicingResult {
        private final SlicingConfig config;
        private final java.util.List<OrderSlice> slices;
        private final int numSlices;
        private final long avgSliceSize;
        private final double estimatedTotalImpact;
        private final long estimatedDuration;
        
        public SlicingResult(SlicingConfig config, java.util.List<OrderSlice> slices) {
            this.config = config;
            this.slices = slices;
            this.numSlices = slices.size();
            
            this.avgSliceSize = (long) slices.stream()
                .mapToLong(OrderSlice::getQuantity)
                .average()
                .orElse(0.0);
            
            // 估算总冲击成本
            this.estimatedTotalImpact = estimateTotalImpact(slices, config.getMaxMarketImpact());
            
            // 估算执行时长
            this.estimatedDuration = config.getTimeHorizonMinutes() * 60L * 1_000_000_000L;
        }
        
        public SlicingConfig getConfig() { return config; }
        public java.util.List<OrderSlice> getSlices() { return slices; }
        public int getNumSlices() { return numSlices; }
        public long getAvgSliceSize() { return avgSliceSize; }
        public double getEstimatedTotalImpact() { return estimatedTotalImpact; }
        public long getEstimatedDuration() { return estimatedDuration; }
        
        @Override
        public String toString() {
            return String.format(
                "SlicingResult{slices=%d, avgSize=%d, impact=%.4f%%, duration=%.1fmin}",
                numSlices, avgSliceSize, estimatedTotalImpact * 100,
                estimatedDuration / 60e9
            );
        }
        
        /**
         * 估算总冲击成本
         */
        private double estimateTotalImpact(java.util.List<OrderSlice> slices, double maxImpact) {
            // 简化模型：假设每次执行的冲击与数量平方根成正比
            double totalImpact = 0.0;
            for (OrderSlice slice : slices) {
                double singleImpact = Math.sqrt(slice.getQuantity()) * 0.0001;
                totalImpact += singleImpact;
            }
            return Math.min(totalImpact, maxImpact);
        }
    }
    
    /**
     * 市场深度信息
     */
    public static class MarketDepth {
        private final long bidVolume;      // 买盘总量
        private final long askVolume;      // 卖盘总量
        private final double spread;       // 价差
        private final double volatility;   // 波动率
        
        public MarketDepth(long bidVolume, long askVolume, double spread, double volatility) {
            this.bidVolume = bidVolume;
            this.askVolume = askVolume;
            this.spread = spread;
            this.volatility = volatility;
        }
        
        public long getBidVolume() { return bidVolume; }
        public long getAskVolume() { return askVolume; }
        public double getSpread() { return spread; }
        public double getVolatility() { return volatility; }
        
        /**
         * 获取综合流动性评分
         */
        public double getLiquidityScore() {
            return (bidVolume + askVolume) / (spread * 100);
        }
    }
    
    /**
     * 主函数 - 根据配置和策略分割订单
     * 
     * @param config 分割配置
     * @param marketDepth 市场深度信息
     * @return 分割结果
     */
    public SlicingResult sliceOrder(SlicingConfig config, MarketDepth marketDepth) {
        java.util.List<OrderSlice> slices;
        
        switch (config.getStrategy()) {
            case EQUAL:
                slices = createEqualSlices(config);
                break;
            case PROPORTIONAL:
                slices = createProportionalSlices(config, marketDepth);
                break;
            case RANDOM:
                slices = createRandomSlices(config);
                break;
            case ADAPTIVE:
            default:
                slices = createAdaptiveSlices(config, marketDepth);
                break;
        }
        
        return new SlicingResult(config, slices);
    }
    
    /**
     * 等分策略 - 将订单均匀分割
     */
    private java.util.List<OrderSlice> createEqualSlices(SlicingConfig config) {
        java.util.List<OrderSlice> slices = new java.util.ArrayList<>();
        
        // 计算最优分割数
        int numSlices = calculateOptimalNumSlices(config);
        long sliceQty = config.getTotalQuantity() / numSlices;
        
        long remainingQty = config.getTotalQuantity();
        long startTime = System.nanoTime();
        long interval = config.getTimeHorizonMinutes() * 60L * 1_000_000_000L / numSlices;
        
        for (int i = 0; i < numSlices; i++) {
            // 最后一片处理剩余数量
            long currentQty = (i == numSlices - 1) ? remainingQty : sliceQty;
            
            // 确保在最小/最大范围内
            currentQty = Math.max(config.getMinSliceQuantity(), 
                         Math.min(currentQty, config.getMaxSliceQuantity()));
            
            OrderSlice slice = new OrderSlice(i, currentQty, 0.0, startTime + i * interval);
            slices.add(slice);
            remainingQty -= currentQty;
        }
        
        return slices;
    }
    
    /**
     * 比例策略 - 根据市场流动性分配
     */
    private java.util.List<OrderSlice> createProportionalSlices(
            SlicingConfig config, MarketDepth marketDepth) {
        
        java.util.List<OrderSlice> slices = new java.util.ArrayList<>();
        
        // 根据流动性确定每次执行量（流动性的 5-10%）
        double liquidityRatio = 0.05 + Math.random() * 0.05;
        long marketVolume = (marketDepth.getBidVolume() + marketDepth.getAskVolume()) / 2;
        long sliceQty = (long) (marketVolume * liquidityRatio);
        
        // 限制在配置范围内
        sliceQty = Math.max(config.getMinSliceQuantity(),
                   Math.min(sliceQty, config.getMaxSliceQuantity()));
        
        int numSlices = (int) Math.ceil((double) config.getTotalQuantity() / sliceQty);
        long startTime = System.nanoTime();
        long interval = config.getTimeHorizonMinutes() * 60L * 1_000_000_000L / numSlices;
        
        long remainingQty = config.getTotalQuantity();
        for (int i = 0; i < numSlices; i++) {
            long currentQty = Math.min(sliceQty, remainingQty);
            OrderSlice slice = new OrderSlice(i, currentQty, 0.0, startTime + i * interval);
            slices.add(slice);
            remainingQty -= currentQty;
        }
        
        return slices;
    }
    
    /**
     * 随机策略 - 随机化每份大小
     */
    private java.util.List<OrderSlice> createRandomSlices(SlicingConfig config) {
        java.util.List<OrderSlice> slices = new java.util.ArrayList<>();
        
        long remainingQty = config.getTotalQuantity();
        int sliceIndex = 0;
        long startTime = System.nanoTime();
        
        while (remainingQty > 0) {
            // 在当前剩余量的 10%-20% 间随机
            double ratio = 0.1 + Math.random() * 0.1;
            long sliceQty = (long) (remainingQty * ratio);
            
            // 限制在配置范围内
            sliceQty = Math.max(config.getMinSliceQuantity(),
                       Math.min(sliceQty, config.getMaxSliceQuantity()));
            
            // 确保不会超过剩余量
            sliceQty = Math.min(sliceQty, remainingQty);
            
            // 随机时间间隔
            long baseInterval = config.getTimeHorizonMinutes() * 60L * 1_000_000_000L / 20;
            long jitter = (long) (baseInterval * 0.3 * Math.random());
            long execTime = startTime + sliceIndex * baseInterval + jitter;
            
            OrderSlice slice = new OrderSlice(sliceIndex++, sliceQty, 0.0, execTime);
            slices.add(slice);
            remainingQty -= sliceQty;
        }
        
        return slices;
    }
    
    /**
     * 自适应策略 - 根据市场状态动态调整
     */
    private java.util.List<OrderSlice> createAdaptiveSlices(
            SlicingConfig config, MarketDepth marketDepth) {
        
        // 高波动性时快速执行，低波动性时慢速执行
        double volatilityFactor = Math.min(2.0, Math.max(0.5, marketDepth.getVolatility() * 10));
        
        // 流动性好时执行量大，流动性差时执行量小
        double liquidityFactor = Math.log10(marketDepth.getLiquidityScore() + 1);
        
        // 计算调整后的分割参数
        int adjustedNumSlices = (int) (calculateOptimalNumSlices(config) * volatilityFactor);
        long baseSliceQty = config.getTotalQuantity() / adjustedNumSlices;
        
        java.util.List<OrderSlice> slices = new java.util.ArrayList<>();
        long remainingQty = config.getTotalQuantity();
        long startTime = System.nanoTime();
        long interval = config.getTimeHorizonMinutes() * 60L * 1_000_000_000L / adjustedNumSlices;
        
        for (int i = 0; i < adjustedNumSlices && remainingQty > 0; i++) {
            // 根据当前市场状态微调
            double adjustment = 1.0 + (Math.random() - 0.5) * 0.2 * liquidityFactor;
            long sliceQty = (long) (baseSliceQty * adjustment);
            
            sliceQty = Math.max(config.getMinSliceQuantity(),
                       Math.min(sliceQty, config.getMaxSliceQuantity()));
            sliceQty = Math.min(sliceQty, remainingQty);
            
            OrderSlice slice = new OrderSlice(i, sliceQty, 0.0, startTime + i * interval);
            slices.add(slice);
            remainingQty -= sliceQty;
        }
        
        // 处理剩余数量
        if (remainingQty > 0) {
            OrderSlice finalSlice = new OrderSlice(
                slices.size(), 
                remainingQty, 
                0.0, 
                startTime + slices.size() * interval
            );
            slices.add(finalSlice);
        }
        
        return slices;
    }
    
    /**
     * 计算最优分割数
     * 
     * 基于经典的 Almgren-Chriss 模型简化版
     */
    private int calculateOptimalNumSlices(SlicingConfig config) {
        // 简化公式：N = sqrt(Q / D)
        // Q: 订单量，D: 市场深度（假设为 10000）
        double marketDepth = 10000.0;
        int optimalNum = (int) Math.sqrt(config.getTotalQuantity() / marketDepth);
        
        // 限制在合理范围内
        return Math.max(5, Math.min(optimalNum, 50));
    }
}
