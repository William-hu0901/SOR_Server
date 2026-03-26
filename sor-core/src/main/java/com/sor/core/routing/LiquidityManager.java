package com.sor.core.routing;

import com.sor.core.domain.MarketLiquidity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流动性管理器
 * 
 * 维护各交易品种的实时市场流动性数据
 * 使用 ConcurrentHashMap 支持高并发访问
 */
public class LiquidityManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(LiquidityManager.class);
    
    // 存储各交易品种的流动性数据
    private final Map<String, MarketLiquidity> liquidityMap;
    
    // 默认交易所数量
    private static final int DEFAULT_EXCHANGE_COUNT = 5;
    
    /**
     * 构造函数
     */
    public LiquidityManager() {
        this.liquidityMap = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取指定交易品种的流动性数据
     */
    public MarketLiquidity getLiquidity(String symbol) {
        return liquidityMap.get(symbol);
    }
    
    /**
     * 更新流动性数据
     */
    public void updateLiquidity(String symbol, MarketLiquidity liquidity) {
        liquidityMap.put(symbol, liquidity);
        LOG.trace("Updated liquidity for {}: {}", symbol, liquidity);
    }
    
    /**
     * 移除交易品种的流动性数据
     */
    public void removeLiquidity(String symbol) {
        liquidityMap.remove(symbol);
    }
    
    /**
     * 初始化模拟流动性数据（用于测试）
     */
    public void initMockLiquidity(String symbol) {
        MarketLiquidity liquidity = new MarketLiquidity(DEFAULT_EXCHANGE_COUNT);
        
        // 模拟 5 个交易所的数据
        for (int i = 0; i < DEFAULT_EXCHANGE_COUNT; i++) {
            double bidPrice = 100.0 + i * 0.01;
            double askPrice = 100.02 + i * 0.01;
            double volume = 10000.0 * (DEFAULT_EXCHANGE_COUNT - i);
            long latency = 1000000L + i * 200000L; // 1ms - 2ms
            
            liquidity.setExchangeData(i, i + 1, bidPrice, askPrice, volume, latency);
        }
        
        updateLiquidity(symbol, liquidity);
        LOG.info("Initialized mock liquidity for {}", symbol);
    }
}
