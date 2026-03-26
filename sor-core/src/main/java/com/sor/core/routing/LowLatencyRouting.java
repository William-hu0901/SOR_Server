package com.sor.core.routing;

import com.sor.core.domain.Order;
import com.sor.core.domain.MarketLiquidity;

/**
 * 低延迟路由策略
 * 
 * 选择网络延迟最低的交易所，适用于对时延敏感的场景
 */
public class LowLatencyRouting implements RoutingStrategy {
    
    @Override
    public int selectExchange(Order order, MarketLiquidity liquidity) {
        int bestExchangeId = -1;
        long minLatency = Long.MAX_VALUE;
        
        // 选择延迟最低的交易所
        for (int i = 0; i < liquidity.getExchangeCount(); i++) {
            long latency = liquidity.getLatency(i);
            if (latency < minLatency) {
                minLatency = latency;
                bestExchangeId = liquidity.getExchangeId(i);
            }
        }
        
        return bestExchangeId;
    }
    
    @Override
    public String name() {
        return "LOW_LATENCY";
    }
}
