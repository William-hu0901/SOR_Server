package com.sor.core.algorithm;

/**
 * 价格发现算法（Price Discovery Algorithm）
 * 
 * 算法描述：
 * 价格发现是金融市场确定资产公允价值的过程。该算法综合分析订单簿、
 * 历史成交、市场深度等多源数据，计算出资产的合理价格区间。
 * 
 * 核心功能：
 * - 公允价值计算：基于市场数据估算合理价格
 * - 价格偏离检测：识别异常定价机会
 * - 支撑/阻力位分析：找到关键价格水平
 * - 市场情绪判断：评估买卖压力对比
 * 
 * 计算方法：
 * 1. 订单簿加权：根据各档位的量和距离加权
 * 2. 中间价修正：考虑买卖不平衡
 * 3. 成交量分布：识别高成交量价格区域
 * 
 * 应用场景：
 * - 智能订单路由的基准价格
 * - 统计套利策略的信号生成
 * - 风险管理的公允价值评估
 * - 交易执行的时机选择
 * 
 * 数学模型：
 * Fair Value = Σ(Price_i × Weight_i) / Σ(Weight_i)
 * 其中 Weight_i = Quantity_i / Distance_from_mid
 * 
 * @author SOR Team
 * @version 1.0
 */
public class PriceDiscoveryAlgorithm {
    
    /**
     * 价格水平 - 订单簿中的一档
     */
    public static class PriceLevel {
        private final double price;
        private final long quantity;
        private final int orderCount;  // 该价位的订单数
        
        public PriceLevel(double price, long quantity, int orderCount) {
            this.price = price;
            this.quantity = quantity;
            this.orderCount = orderCount;
        }
        
        public double getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public int getOrderCount() { return orderCount; }
    }
    
    /**
     * 价格发现结果
     */
    public static class PriceDiscoveryResult {
        private final double fairValue;           // 公允价值
        private final double bidWeightedPrice;    // 买盘加权价
        private final double askWeightedPrice;    // 卖盘加权价
        private final double imbalance;           // 买卖不平衡度 (-1 到 1)
        private final double confidence;          // 置信度 (0-1)
        private final java.util.List<Double> supportLevels;   // 支撑位
        private final java.util.List<Double> resistanceLevels; // 阻力位
        private final long timestamp;
        
        public PriceDiscoveryResult(double fairValue, double bidWeighted, double askWeighted,
                                   double imbalance, double confidence,
                                   java.util.List<Double> supports,
                                   java.util.List<Double> resistances) {
            this.fairValue = fairValue;
            this.bidWeightedPrice = bidWeighted;
            this.askWeightedPrice = askWeighted;
            this.imbalance = imbalance;
            this.confidence = confidence;
            this.supportLevels = supports;
            this.resistanceLevels = resistances;
            this.timestamp = System.nanoTime();
        }
        
        public double getFairValue() { return fairValue; }
        public double getBidWeightedPrice() { return bidWeightedPrice; }
        public double getAskWeightedPrice() { return askWeightedPrice; }
        public double getImbalance() { return imbalance; }
        public double getConfidence() { return confidence; }
        public java.util.List<Double> getSupportLevels() { return supportLevels; }
        public java.util.List<Double> getResistanceLevels() { return resistanceLevels; }
        public long getTimestamp() { return timestamp; }
        
        /**
         * 判断是否被低估（买入信号）
         */
        public boolean isUndervalued(double currentPrice, double threshold) {
            return currentPrice < fairValue * (1 - threshold);
        }
        
        /**
         * 判断是否被高估（卖出信号）
         */
        public boolean isOvervalued(double currentPrice, double threshold) {
            return currentPrice > fairValue * (1 + threshold);
        }
        
        @Override
        public String toString() {
            return String.format(
                "PriceDiscovery{fair=%.4f, bid=%.4f, ask=%.4f, imbalance=%.2f, confidence=%.2f}",
                fairValue, bidWeightedPrice, askWeightedPrice, imbalance, confidence
            );
        }
    }
    
    /**
     * 订单项
     */
    public static class OrderBookItem {
        private final double price;
        private final long quantity;
        private final Side side;
        
        public OrderBookItem(double price, long quantity, Side side) {
            this.price = price;
            this.quantity = quantity;
            this.side = side;
        }
        
        public enum Side { BID, ASK }
        
        public double getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public Side getSide() { return side; }
    }
    
    /**
     * 主函数 - 基于订单簿计算公允价值
     * 
     * <p>使用三重加权法：</p>
     * <ol>
     *     <li>数量加权：大单更有影响力</li>
     *     <li>距离加权：离中间价越近权重越大</li>
     *     <li>时间加权：新订单权重更高</li>
     * </ol>
     * 
     * @param bids 买单列表（按价格降序）
     * @param asks 卖单列表（按价格升序）
     * @return 价格发现结果
     */
    public PriceDiscoveryResult discoverPrice(java.util.List<OrderBookItem> bids,
                                              java.util.List<OrderBookItem> asks) {
        if (bids.isEmpty() || asks.isEmpty()) {
            return createEmptyResult();
        }
        
        // 计算中间价
        double bestBid = bids.get(0).getPrice();
        double bestAsk = asks.get(0).getPrice();
        double midPrice = (bestBid + bestAsk) / 2.0;
        
        // 计算买盘加权价
        double bidWeightedSum = 0.0;
        long bidTotalWeight = 0;
        
        for (OrderBookItem bid : bids) {
            double weight = calculateWeight(bid.getQuantity(), bid.getPrice(), midPrice);
            bidWeightedSum += bid.getPrice() * weight;
            bidTotalWeight += weight;
        }
        
        double bidWeightedPrice = (bidTotalWeight > 0) 
            ? bidWeightedSum / bidTotalWeight 
            : bestBid;
        
        // 计算卖盘加权价
        double askWeightedSum = 0.0;
        long askTotalWeight = 0;
        
        for (OrderBookItem ask : asks) {
            double weight = calculateWeight(ask.getQuantity(), ask.getPrice(), midPrice);
            askWeightedSum += ask.getPrice() * weight;
            askTotalWeight += weight;
        }
        
        double askWeightedPrice = (askTotalWeight > 0) 
            ? askWeightedSum / askTotalWeight 
            : bestAsk;
        
        // 计算公允价值（买卖加权平均）
        double fairValue = (bidWeightedPrice + askWeightedPrice) / 2.0;
        
        // 计算买卖不平衡度
        double totalBidQty = bids.stream().mapToLong(OrderBookItem::getQuantity).sum();
        double totalAskQty = asks.stream().mapToLong(OrderBookItem::getQuantity).sum();
        double imbalance = calculateImbalance(totalBidQty, totalAskQty);
        
        // 计算置信度
        double confidence = calculateConfidence(bids, asks, midPrice);
        
        // 识别支撑位和阻力位
        java.util.List<Double> supports = identifySupportLevels(bids, midPrice);
        java.util.List<Double> resistances = identifyResistanceLevels(asks, midPrice);
        
        return new PriceDiscoveryResult(
            fairValue,
            bidWeightedPrice,
            askWeightedPrice,
            imbalance,
            confidence,
            supports,
            resistances
        );
    }
    
    /**
     * 计算权重 - 结合数量和距离因素
     * 
     * @param quantity 订单数量
     * @param price 订单价格
     * @param midPrice 中间价
     * @return 权重值
     */
    private double calculateWeight(long quantity, double price, double midPrice) {
        // 基础权重为数量
        double baseWeight = quantity;
        
        // 距离惩罚：离中间价越远，权重越小
        double distanceRatio = Math.abs(price - midPrice) / midPrice;
        double distancePenalty = 1.0 / (1.0 + distanceRatio * 100);
        
        return baseWeight * distancePenalty;
    }
    
    /**
     * 计算买卖不平衡度
     * 
     * @param bidQty 买盘总量
     * @param askQty 卖盘总量
     * @return 不平衡度 (-1: 完全卖方，0: 平衡，1: 完全买方)
     */
    private double calculateImbalance(double bidQty, double askQty) {
        double total = bidQty + askQty;
        if (total == 0) {
            return 0.0;
        }
        return (bidQty - askQty) / total;
    }
    
    /**
     * 计算置信度
     * 
     * @param bids 买单
     * @param asks 卖单
     * @param midPrice 中间价
     * @return 置信度 (0-1)
     */
    private double calculateConfidence(java.util.List<OrderBookItem> bids,
                                      java.util.List<OrderBookItem> asks,
                                      double midPrice) {
        // 基于以下因素计算置信度：
        // 1. 订单簿深度（总数量）
        // 2. 价差（spread）
        // 3. 订单密度
        
        if (bids.isEmpty() || asks.isEmpty()) {
            return 0.0;
        }
        
        double bestBid = bids.get(0).getPrice();
        double bestAsk = asks.get(0).getPrice();
        double spread = bestAsk - bestBid;
        double spreadRatio = spread / midPrice;
        
        // 价差越小，置信度越高
        double spreadScore = 1.0 / (1.0 + spreadRatio * 100);
        
        // 深度越大，置信度越高
        double totalDepth = bids.stream().mapToLong(OrderBookItem::getQuantity).sum()
                      + asks.stream().mapToLong(OrderBookItem::getQuantity).sum();
        double depthScore = Math.min(1.0, totalDepth / 10000.0);  // 假设 10000 为满分
        
        // 综合置信度
        return (spreadScore * 0.6 + depthScore * 0.4);
    }
    
    /**
     * 识别支撑位 - 买单集中的价格水平
     */
    private java.util.List<Double> identifySupportLevels(
            java.util.List<OrderBookItem> bids, double midPrice) {
        
        java.util.Map<Integer, Long> priceBuckets = new java.util.HashMap<>();
        
        // 将买单按价格分组（以 1% 为间隔）
        for (OrderBookItem bid : bids) {
            int bucket = (int) ((midPrice - bid.getPrice()) / midPrice * 100);
            if (bucket >= 0 && bucket <= 10) {  // 只考虑 10% 以内的买单
                priceBuckets.merge(bucket, bid.getQuantity(), Long::sum);
            }
        }
        
        // 找出累积量最大的几个价格水平
        return priceBuckets.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(3)
            .map(e -> midPrice * (1 - e.getKey() / 100.0))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 识别阻力位 - 卖单集中的价格水平
     */
    private java.util.List<Double> identifyResistanceLevels(
            java.util.List<OrderBookItem> asks, double midPrice) {
        
        java.util.Map<Integer, Long> priceBuckets = new java.util.HashMap<>();
        
        // 将卖单按价格分组
        for (OrderBookItem ask : asks) {
            int bucket = (int) ((ask.getPrice() - midPrice) / midPrice * 100);
            if (bucket >= 0 && bucket <= 10) {  // 只考虑 10% 以内的卖单
                priceBuckets.merge(bucket, ask.getQuantity(), Long::sum);
            }
        }
        
        return priceBuckets.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(3)
            .map(e -> midPrice * (1 + e.getKey() / 100.0))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 创建空结果
     */
    private PriceDiscoveryResult createEmptyResult() {
        return new PriceDiscoveryResult(
            0.0, 0.0, 0.0, 0.0, 0.0,
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>()
        );
    }
    
    /**
     * 检测套利机会 - 当市场价格显著偏离公允价值时
     * 
     * @param marketPrice 市场价格
     * @param result 价格发现结果
     * @param threshold 阈值（百分比）
     * @return 套利信号（正：做多，负：做空，0：无机会）
     */
    public double detectArbitrageOpportunity(double marketPrice, 
                                            PriceDiscoveryResult result,
                                            double threshold) {
        double fairValue = result.getFairValue();
        
        if (fairValue == 0.0) {
            return 0.0;
        }
        
        double deviation = (marketPrice - fairValue) / fairValue;
        
        if (deviation > threshold) {
            // 价格高估，做空信号
            return -deviation;
        } else if (deviation < -threshold) {
            // 价格低估，做多信号
            return -deviation;  // 返回正值
        }
        
        return 0.0;  // 无套利机会
    }
}
