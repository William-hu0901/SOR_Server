package com.sor.core.routing;

import com.sor.core.domain.Order;
import com.sor.core.domain.MarketLiquidity;

/**
 * 路由策略接口
 */
public interface RoutingStrategy {
    
    /**
     * 选择最优交易所
     * @param order 订单
     * @param liquidity 市场流动性数据
     * @return 交易所 ID
     */
    int selectExchange(Order order, MarketLiquidity liquidity);
    
    /**
     * 策略名称
     */
    String name();
}
