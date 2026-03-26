package com.sor.core.routing;

import com.sor.core.domain.MarketLiquidity;
import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;

/**
 * 最优价格路由策略
 * 
 * 选择提供最优成交价格的交易所
 * -买单：选择最低卖价
 * -卖单：选择最高买价
 */
public class BestPriceRouting implements RoutingStrategy {
    
    @Override
    public int selectExchange(Order order, MarketLiquidity liquidity) {
        int bestExchangeId = -1;
        double bestPrice = order.getSide() == OrderSide.BUY ? Double.MAX_VALUE : Double.MIN_VALUE;
        
        // 遍历所有交易所的报价
        for (int i = 0; i < liquidity.getExchangeCount(); i++) {
            double price = order.getSide() == OrderSide.BUY 
                    ? liquidity.getAskPrice(i) 
                    : liquidity.getBidPrice(i);
            
            boolean isBetter = order.getSide() == OrderSide.BUY 
                    ? price < bestPrice 
                    : price > bestPrice;
            
            if (isBetter) {
                bestPrice = price;
                bestExchangeId = liquidity.getExchangeId(i);
            }
        }
        
        return bestExchangeId;
    }
    
    @Override
    public String name() {
        return "BEST_PRICE";
    }
}
