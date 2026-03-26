package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 价格限制检查器
 * 
 * 检查订单价格是否在合理范围内
 */
public class PriceLimitChecker implements RiskChecker {
    
    private static final Logger LOG = LoggerFactory.getLogger(PriceLimitChecker.class);
    
    // 最大价格偏离百分比（默认 10%）
    private final double maxDeviationPercent;
    
    // 参考价格（通常是最新市场价）
    private volatile double referencePrice;
    
    /**
     * 构造函数
     */
    public PriceLimitChecker(double maxDeviationPercent) {
        this.maxDeviationPercent = maxDeviationPercent;
        this.referencePrice = 0.0;
    }
    
    /**
     * 设置参考价格
     */
    public void setReferencePrice(double referencePrice) {
        this.referencePrice = referencePrice;
        LOG.debug("Updated reference price: {}", referencePrice);
    }
    
    @Override
    public RiskCheckResult check(Order order) {
        // 市价单不需要价格检查
        if (order.getPrice() == 0.0) {
            return RiskCheckResult.success();
        }
        
        // 如果没有参考价格，跳过检查
        if (referencePrice <= 0.0) {
            return RiskCheckResult.success();
        }
        
        // 计算价格偏离百分比
        double deviation = Math.abs(order.getPrice() - referencePrice) / referencePrice * 100.0;
        
        if (deviation > maxDeviationPercent) {
            LOG.warn("Price limit exceeded: orderPrice={}, refPrice={}, deviation={:.2f}%",
                    order.getPrice(), referencePrice, deviation);
            return RiskCheckResult.fail(
                String.format("Price deviation %.2f%% exceeds limit %.2f%%", 
                            deviation, maxDeviationPercent));
        }
        
        LOG.trace("Price check passed: {}@{}", order.getSymbol(), order.getPrice());
        return RiskCheckResult.success();
    }
}
