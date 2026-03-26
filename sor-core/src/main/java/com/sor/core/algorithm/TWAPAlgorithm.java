package com.sor.core.algorithm;

import java.util.concurrent.atomic.AtomicLong;

/**
 * TWAP 时间加权平均价格算法（Time Weighted Average Price）
 * 
 * 算法描述：
 * TWAP 是一种按时间均匀分割订单的执行策略，目标是在指定时间段内以市场平均价格成交。
 * 与 VWAP 不同，TWAP 不考虑成交量权重，而是简单地在时间维度上平均分配订单。
 * 
 * 核心思想：
 * 1. 将总订单数量均匀分成 N 份
 * 2. 在固定时间间隔 T 内执行一份
 * 3. 最终成交价即为 TWAP
 * 
 * 计算公式：
 * TWAP = (P1 + P2 + ... + Pn) / n
 * 其中：Pi 为第 i 次成交的价格，n 为成交次数
 * 
 * 优势：
 * - 简单透明：易于理解和实施
 * - 减少冲击：避免大单对市场的冲击
 * - 降低风险：分散 timing risk
 * 
 * 劣势：
 * - 可预测性：容易被其他交易者识别和利用
 * - 忽略流动性：在低流动性时段可能造成较大冲击
 * 
 * 应用场景：
 * - 大额订单的渐进式执行
 * - 流动性较差的交易品种
 * - 需要隐藏交易意图的场景
 * - 定期定额投资计划
 * 
 * 优化策略：
 * - 随机化时间间隔（避免被识别）
 * - 根据流动性动态调整单次执行量
 * - 设置价格限制条件
 * 
 * @author SOR Team
 * @version 1.0
 */
public class TWAPAlgorithm {
    
    /**
     * TWAP 订单配置
     */
    public static class TWAPConfig {
        private final long totalQuantity;           // 总订单数量
        private final int numSlices;                // 分割份数
        private final long intervalNanos;           // 时间间隔（纳秒）
        private final double priceTolerance;        // 价格容忍度（百分比）
        private final boolean randomizeTiming;      // 是否随机化时间
        
        public TWAPConfig(long totalQuantity, int numSlices, long intervalNanos) {
            this(totalQuantity, numSlices, intervalNanos, 0.02, false);
        }
        
        public TWAPConfig(long totalQuantity, int numSlices, long intervalNanos,
                         double priceTolerance, boolean randomizeTiming) {
            this.totalQuantity = totalQuantity;
            this.numSlices = numSlices;
            this.intervalNanos = intervalNanos;
            this.priceTolerance = priceTolerance;
            this.randomizeTiming = randomizeTiming;
        }
        
        public long getTotalQuantity() { return totalQuantity; }
        public int getNumSlices() { return numSlices; }
        public long getIntervalNanos() { return intervalNanos; }
        public double getPriceTolerance() { return priceTolerance; }
        public boolean isRandomizeTiming() { return randomizeTiming; }
        
        /**
         * 计算每份的数量
         */
        public long getSliceQuantity() {
            return (long) Math.ceil((double) totalQuantity / numSlices);
        }
        
        /**
         * 计算总执行时间（毫秒）
         */
        public long getTotalDurationMillis() {
            return (intervalNanos * numSlices) / 1_000_000;
        }
    }
    
    /**
     * TWAP 执行片段
     */
    public static class TWAPSlice {
        private final int sliceIndex;
        private final long quantity;
        private final long scheduledTime;
        private Double executedPrice;
        private long executedQuantity;
        private long executionTime;
        private boolean isExecuted;
        
        public TWAPSlice(int sliceIndex, long quantity, long scheduledTime) {
            this.sliceIndex = sliceIndex;
            this.quantity = quantity;
            this.scheduledTime = scheduledTime;
            this.isExecuted = false;
        }
        
        public void execute(double price, long execQty, long execTime) {
            this.executedPrice = price;
            this.executedQuantity = execQty;
            this.executionTime = execTime;
            this.isExecuted = true;
        }
        
        public int getSliceIndex() { return sliceIndex; }
        public long getQuantity() { return quantity; }
        public long getScheduledTime() { return scheduledTime; }
        public Double getExecutedPrice() { return executedPrice; }
        public long getExecutedQuantity() { return executedQuantity; }
        public long getExecutionTime() { return executionTime; }
        public boolean isExecuted() { return isExecuted; }
        public boolean isPartiallyFilled() { 
            return isExecuted && executedQuantity < quantity; 
        }
    }
    
    /**
     * TWAP 执行结果
     */
    public static class TWAPResult {
        private final TWAPConfig config;
        private final java.util.List<TWAPSlice> slices;
        private final double twap;
        private final long totalExecutedQuantity;
        private final double averagePrice;
        private final long startTime;
        private final long endTime;
        
        public TWAPResult(TWAPConfig config, java.util.List<TWAPSlice> slices) {
            this.config = config;
            this.slices = slices;
            
            // 计算 TWAP 和统计信息
            double priceSum = 0.0;
            long qtySum = 0;
            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;
            
            for (TWAPSlice slice : slices) {
                if (slice.isExecuted() && slice.getExecutedPrice() != null) {
                    priceSum += slice.getExecutedPrice();
                    qtySum += slice.getExecutedQuantity();
                    
                    if (slice.getExecutionTime() < minTime) {
                        minTime = slice.getExecutionTime();
                    }
                    if (slice.getExecutionTime() > maxTime) {
                        maxTime = slice.getExecutionTime();
                    }
                }
            }
            
            int executedCount = (int) slices.stream().filter(TWAPSlice::isExecuted).count();
            this.twap = (executedCount > 0) ? priceSum / executedCount : 0.0;
            this.totalExecutedQuantity = qtySum;
            this.averagePrice = (qtySum > 0) ? priceSum / executedCount : 0.0;
            this.startTime = minTime == Long.MAX_VALUE ? 0 : minTime;
            this.endTime = maxTime == Long.MIN_VALUE ? 0 : maxTime;
        }
        
        public TWAPConfig getConfig() { return config; }
        public java.util.List<TWAPSlice> getSlices() { return slices; }
        public double getTWAP() { return twap; }
        public long getTotalExecutedQuantity() { return totalExecutedQuantity; }
        public double getAveragePrice() { return averagePrice; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public int getTotalSlices() { return slices.size(); }
        public int getExecutedSlices() { 
            return (int) slices.stream().filter(TWAPSlice::isExecuted).count(); 
        }
        public double getExecutionRate() {
            return (double) getExecutedSlices() / slices.size() * 100.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "TWAP{twap=%.4f, avg=%.4f, executed=%d/%d (%.1f%%), qty=%d}",
                twap, averagePrice, getExecutedSlices(), getTotalSlices(),
                getExecutionRate(), totalExecutedQuantity
            );
        }
    }
    
    /**
     * TWAP 调度器 - 生成执行计划
     */
    public static class TWAPScheduler {
        private final TWAPConfig config;
        private final AtomicLong currentSliceIndex;
        
        public TWAPScheduler(TWAPConfig config) {
            this.config = config;
            this.currentSliceIndex = new AtomicLong(0);
        }
        
        /**
         * 生成完整的 TWAP 执行计划
         * 
         * @param startTime 开始时间（纳秒）
         * @return TWAP 切片列表
         */
        public java.util.List<TWAPSlice> generateSchedule(long startTime) {
            java.util.List<TWAPSlice> slices = new java.util.ArrayList<>();
            long sliceQty = config.getSliceQuantity();
            long remainingQty = config.getTotalQuantity();
            
            for (int i = 0; i < config.getNumSlices(); i++) {
                // 计算当前片的数量（最后一片处理剩余数量）
                long currentQty = (i == config.getNumSlices() - 1) 
                    ? remainingQty 
                    : Math.min(sliceQty, remainingQty);
                
                // 计算计划执行时间
                long scheduledTime = startTime + (i * config.getIntervalNanos());
                
                // 如果启用随机化，添加小的时间扰动
                if (config.isRandomizeTiming()) {
                    scheduledTime = addTimeJitter(scheduledTime, config.getIntervalNanos());
                }
                
                slices.add(new TWAPSlice(i, currentQty, scheduledTime));
                remainingQty -= currentQty;
            }
            
            return slices;
        }
        
        /**
         * 获取下一片应该执行的时间
         * 
         * @param startTime 开始时间
         * @return 下一次执行时间
         */
        public long getNextExecutionTime(long startTime) {
            long index = currentSliceIndex.getAndIncrement();
            long nextTime = startTime + (index * config.getIntervalNanos());
            
            if (config.isRandomizeTiming()) {
                nextTime = addTimeJitter(nextTime, config.getIntervalNanos());
            }
            
            return nextTime;
        }
        
        /**
         * 添加时间抖动（±10%）
         */
        private long addTimeJitter(long baseTime, long interval) {
            double jitter = (Math.random() - 0.5) * 0.2 * interval;
            return baseTime + (long) jitter;
        }
        
        /**
         * 重置调度器
         */
        public void reset() {
            currentSliceIndex.set(0);
        }
    }
    
    /**
     * 主函数 - 执行完整的 TWAP 策略（模拟）
     * 
     * @param config TWAP 配置
     * @param priceProvider 价格提供者函数接口
     * @return TWAP 执行结果
     */
    public TWAPResult execute(TWAPConfig config, java.util.function.DoubleSupplier priceProvider) {
        TWAPScheduler scheduler = new TWAPScheduler(config);
        java.util.List<TWAPSlice> slices = scheduler.generateSchedule(System.nanoTime());
        
        // 模拟执行每个切片
        for (TWAPSlice slice : slices) {
            // 等待到计划时间（实际应用中需要异步调度）
            // waitForTime(slice.getScheduledTime());
            
            // 获取市场价格并执行
            double marketPrice = priceProvider.getAsDouble();
            
            // 检查价格是否在容忍范围内
            if (isPriceAcceptable(marketPrice, config.getPriceTolerance())) {
                slice.execute(marketPrice, slice.getQuantity(), System.nanoTime());
            } else {
                // 价格超出容忍范围，部分执行或跳过
                long partialQty = slice.getQuantity() / 2;
                if (partialQty > 0) {
                    slice.execute(marketPrice, partialQty, System.nanoTime());
                }
            }
        }
        
        return new TWAPResult(config, slices);
    }
    
    /**
     * 检查价格是否可接受
     */
    private boolean isPriceAcceptable(double currentPrice, double tolerance) {
        // 简化实现：总是返回 true
        // 实际应用可以比较当前价格与基准价格的偏离
        return true;
    }
    
    /**
     * 计算 TWAP 与 VWAP 的对比
     * 
     * @param twap TWAP 值
     * @param vwap VWAP 值
     * @return 差异百分比
     */
    public double compareWithVWAP(double twap, double vwap) {
        if (vwap == 0.0) {
            return 0.0;
        }
        return ((twap - vwap) / vwap) * 100.0;
    }
}
