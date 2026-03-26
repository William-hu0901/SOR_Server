package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 风控检查器接口
 */
public interface RiskChecker {
    
    /**
     * 检查订单是否合法
     * @param order 订单
     * @return 检查结果
     */
    RiskCheckResult check(Order order);
    
    /**
     * 风控检查结果
     */
    class RiskCheckResult {
        
        // 是否通过
        private final boolean passed;
        
        // 失败原因
        private final String reason;
        
        /**
         * 构造函数
         */
        public RiskCheckResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }
        
        /**
         * 创建成功结果
         */
        public static RiskCheckResult success() {
            return new RiskCheckResult(true, null);
        }
        
        /**
         * 创建失败结果
         */
        public static RiskCheckResult fail(String reason) {
            return new RiskCheckResult(false, reason);
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return passed ? "PASS" : "FAIL: " + reason;
        }
    }
}
