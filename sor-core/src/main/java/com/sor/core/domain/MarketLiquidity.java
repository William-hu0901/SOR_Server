package com.sor.core.domain;

/**
 * 市场流动性数据
 * 
 * 封装各交易所的实时报价和深度信息
 * 设计为轻量级对象，可频繁创建用于快照
 */
public final class MarketLiquidity {
    
    // 交易所数量
    private final int exchangeCount;
    
    // 交易所 ID 数组
    private final int[] exchangeIds;
    
    // 买价数组（最高买入价）
    private final double[] bidPrices;
    
    // 卖价数组（最低卖出价）
    private final double[] askPrices;
    
    // 可用量数组
    private final double[] volumes;
    
    // 延迟数组（纳秒）
    private final long[] latencies;
    
    /**
     * 构造函数
     */
    public MarketLiquidity(int exchangeCount) {
        this.exchangeCount = exchangeCount;
        this.exchangeIds = new int[exchangeCount];
        this.bidPrices = new double[exchangeCount];
        this.askPrices = new double[exchangeCount];
        this.volumes = new double[exchangeCount];
        this.latencies = new long[exchangeCount];
    }
    
    /**
     * 设置交易所数据
     */
    public void setExchangeData(int index, int exchangeId, double bidPrice, 
                                double askPrice, double volume, long latency) {
        if (index >= 0 && index < exchangeCount) {
            exchangeIds[index] = exchangeId;
            bidPrices[index] = bidPrice;
            askPrices[index] = askPrice;
            volumes[index] = volume;
            latencies[index] = latency;
        }
    }
    
    // ==================== Getter 方法 ====================
    
    public int getExchangeCount() {
        return exchangeCount;
    }
    
    public int getExchangeId(int index) {
        return exchangeIds[index];
    }
    
    public double getBidPrice(int index) {
        return bidPrices[index];
    }
    
    public double getAskPrice(int index) {
        return askPrices[index];
    }
    
    public double getAvailableVolume(int index) {
        return volumes[index];
    }
    
    public long getLatency(int index) {
        return latencies[index];
    }
    
    /**
     * 检查是否有流动性
     */
    public boolean isEmpty() {
        return exchangeCount == 0;
    }
    
    /**
     * 计算买卖价差
     */
    public double getSpread(int index) {
        return askPrices[index] - bidPrices[index];
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MarketLiquidity{");
        for (int i = 0; i < exchangeCount; i++) {
            sb.append(String.format("\n  Exchange %d: bid=%.2f, ask=%.2f, vol=%.0f, spread=%.4f",
                    exchangeIds[i], bidPrices[i], askPrices[i], volumes[i], getSpread(i)));
        }
        sb.append("\n}");
        return sb.toString();
    }
}
