package com.sor.core.routing;

import com.sor.core.domain.Order;
import com.sor.core.domain.MarketLiquidity;
import com.sor.core.domain.OrderSide;

/**
 * 成交量加权平均价格路由策略
 * 
 * 将大单拆分至多个交易所，按照各交易所的深度分配成交量
 * 减少市场冲击成本
 */
public class VWAPRouting implements RoutingStrategy {
    
    @Override
    public int selectExchange(Order order, MarketLiquidity liquidity) {
        int bestExchangeId = -1;
        double bestScore = -1.0;
        
        // 计算各交易所的 VWAP 分数（价格 * 可用量的倒数）
        for (int i = 0; i < liquidity.getExchangeCount(); i++) {
            double price = order.getSide() == OrderSide.BUY 
                    ? liquidity.getAskPrice(i) 
                    : liquidity.getBidPrice(i);
            double volume = liquidity.getAvailableVolume(i);
            
            // VWAP 分数：价格越低、量越大越好
            double score = volume / price;
            
            if (score > bestScore) {
                bestScore = score;
                bestExchangeId = liquidity.getExchangeId(i);
            }
        }
        
        return bestExchangeId;
    }
    
    @Override
    public String name() {
        return "VWAP";
    }
}
