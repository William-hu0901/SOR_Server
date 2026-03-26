package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 合规规则引擎
 * 
 * 组合多个风控检查器，执行全面的风险检查
 */
public class ComplianceEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(ComplianceEngine.class);
    
    // 风控检查器列表
    final List<RiskChecker> checkers;
    
    /**
     * 构造函数
     */
    public ComplianceEngine() {
        this.checkers = new ArrayList<>();
    }
    
    /**
     * 添加风控检查器
     */
    public void addChecker(RiskChecker checker) {
        checkers.add(checker);
        LOG.info("Added risk checker: {}", checker.getClass().getSimpleName());
    }
    
    /**
     * 移除风控检查器
     */
    public void removeChecker(RiskChecker checker) {
        checkers.remove(checker);
        LOG.info("Removed risk checker: {}", checker.getClass().getSimpleName());
    }
    
    /**
     * 执行所有检查
     * @param order 订单
     * @return 检查结果（全部通过才返回成功）
     */
    public RiskChecker.RiskCheckResult check(Order order) {
        if (checkers.isEmpty()) {
            LOG.debug("No checkers configured, order auto-approved");
            return RiskChecker.RiskCheckResult.success();
        }
        
        // 依次执行所有检查器
        for (RiskChecker checker : checkers) {
            try {
                RiskChecker.RiskCheckResult result = checker.check(order);
                if (!result.isPassed()) {
                    LOG.warn("Order {} failed check {}: {}", 
                            order.getOrderId(), 
                            checker.getClass().getSimpleName(),
                            result.getReason());
                    return result;
                }
            } catch (Exception e) {
                LOG.error("Exception in risk checker: {}", checker.getClass().getSimpleName(), e);
                // 异常情况下拒绝订单（安全优先）
                return RiskChecker.RiskCheckResult.fail(
                    "Risk check exception: " + e.getMessage());
            }
        }
        
        LOG.trace("Order {} passed all risk checks", order.getOrderId());
        return RiskChecker.RiskCheckResult.success();
    }
    
    /**
     * 获取检查器数量
     */
    public int getCheckerCount() {
        return checkers.size();
    }
    
    /**
     * 清空所有检查器
     */
    public void clearCheckers() {
        checkers.clear();
        LOG.info("Cleared all risk checkers");
    }
}
