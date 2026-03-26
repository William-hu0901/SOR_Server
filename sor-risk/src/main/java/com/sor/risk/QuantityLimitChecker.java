package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数量限制检查器
 * 
 * 检查订单数量是否在允许范围内
 */
public class QuantityLimitChecker implements RiskChecker {
    
    private static final Logger LOG = LoggerFactory.getLogger(QuantityLimitChecker.class);
    
    // 最小订单数量
    private final int minQuantity;
    
    // 最大订单数量
    private final int maxQuantity;
    
    /**
     * 构造函数
     */
    public QuantityLimitChecker(int minQuantity, int maxQuantity) {
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }
    
    @Override
    public RiskCheckResult check(Order order) {
        int quantity = order.getQuantity();
        
        // 检查最小数量
        if (quantity < minQuantity) {
            LOG.warn("Quantity below minimum: {} < {}", quantity, minQuantity);
            return RiskCheckResult.fail(
                String.format("Order quantity %d below minimum %d", quantity, minQuantity));
        }
        
        // 检查最大数量
        if (quantity > maxQuantity) {
            LOG.warn("Quantity exceeds maximum: {} > {}", quantity, maxQuantity);
            return RiskCheckResult.fail(
                String.format("Order quantity %d exceeds maximum %d", quantity, maxQuantity));
        }
        
        LOG.trace("Quantity check passed: qty={}", quantity);
        return RiskCheckResult.success();
    }
}
