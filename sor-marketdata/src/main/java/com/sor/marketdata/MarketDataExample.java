package com.sor.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MarketData 模块使用示例
 */
public class MarketDataExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(MarketDataExample.class);
    
    public static void main(String[] args) {
        // 创建市场数据管理器
        MarketDataManager manager = new MarketDataManager();
        
        // 更新 AAPL 的订单簿
        String symbol = "AAPL";
        
        // 添加买单（价格降序）
        manager.updateBid(symbol, 150.0, 100);
        manager.updateBid(symbol, 149.5, 200);
        manager.updateBid(symbol, 149.0, 150);
        
        // 添加卖单（价格升序）
        manager.updateAsk(symbol, 150.5, 50);
        manager.updateAsk(symbol, 151.0, 100);
        manager.updateAsk(symbol, 151.5, 75);
        
        // 获取市场数据
        double bestBid = manager.getBestBid(symbol);
        double bestAsk = manager.getBestAsk(symbol);
        double spread = manager.getSpread(symbol);
        double midPrice = manager.getMidPrice(symbol);
        
        LOG.info("=== {} Market Data ===", symbol);
        LOG.info("Best Bid: ${}", bestBid);
        LOG.info("Best Ask: ${}", bestAsk);
        LOG.info("Spread: ${}", spread);
        LOG.info("Mid Price: ${}", midPrice);
        
        // 打印订单簿快照
        manager.printOrderBookSnapshot(symbol);
        
        // 更新最新成交价
        manager.updateLastPrice(symbol, 150.25);
        
        // 移除某个价格水平
        manager.updateBid(symbol, 149.0, 0);  // quantity=0 表示移除
        
        LOG.info("Updated order book after removal");
        manager.printOrderBookSnapshot(symbol);
    }
}
