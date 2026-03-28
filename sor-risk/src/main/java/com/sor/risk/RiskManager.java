package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

/**
 * 风险管理器
 * 
 * 统一管理所有风控逻辑，提供简化的 API
 * 
 * 使用枚举实现单例模式，确保线程安全且无需同步锁
 * 枚举单例由JVM保证线程安全，且序列化机制保证单例唯一性
 */
public enum RiskManager {

    INSTANCE;
    
    private static final Logger LOG = LoggerFactory.getLogger(RiskManager.class);
    
    // 单例模式（使用枚举实现，线程安全且无需同步锁）
    
    // 合规引擎
    private final ComplianceEngine complianceEngine;
    
    // 统计计数器
    private final LongAdder totalChecked;
    private final LongAdder totalApproved;
    private final LongAdder totalRejected;
    
    /**
     * 私有构造函数（单例）
     */
    private RiskManager() {
        this.complianceEngine = new ComplianceEngine();
        this.totalChecked = new LongAdder();
        this.totalApproved = new LongAdder();
        this.totalRejected = new LongAdder();
        
        // 初始化默认风控规则
        initDefaultRules();
    }
    
    /**
     * 获取单例实例
     */
    public static RiskManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 初始化默认风控规则
     */
    private void initDefaultRules() {
        // 1. 价格限制检查（最大偏离 10%）
        PriceLimitChecker priceChecker = new PriceLimitChecker(10.0);
        complianceEngine.addChecker(priceChecker);
        
        // 2. 数量限制检查（1-10000）
        QuantityLimitChecker quantityChecker = new QuantityLimitChecker(1, 10000);
        complianceEngine.addChecker(quantityChecker);
        
        // 3. 频率限制检查
        // 单个交易者：100 单/秒
        // 单个交易品种：500 单/秒
        // 全局：1000 单/秒
        FrequencyLimitChecker freqChecker = new FrequencyLimitChecker(100, 500, 1000);
        complianceEngine.addChecker(freqChecker);
        
        LOG.info("Initialized default risk rules");
    }
    
    /**
     * 检查订单（主要方法）
     * @param order 订单
     * @return 是否通过
     */
    public boolean checkOrder(Order order) {
        totalChecked.increment();
        
        RiskChecker.RiskCheckResult result = complianceEngine.check(order);
        
        if (result.isPassed()) {
            totalApproved.increment();
            return true;
        } else {
            totalRejected.increment();
            LOG.info("Order {} rejected: {}", order.getOrderId(), result.getReason());
            return false;
        }
    }
    
    /**
     * 更新参考价格（供价格检查使用）
     */
    public void updateReferencePrice(String symbol, double price) {
        // 查找价格检查器并更新参考价格
        for (RiskChecker checker : complianceEngine.checkers) {
            if (checker instanceof PriceLimitChecker) {
                ((PriceLimitChecker) checker).setReferencePrice(price);
            }
        }
    }
    
    /**
     * 添加自定义风控检查器
     */
    public void addChecker(RiskChecker checker) {
        complianceEngine.addChecker(checker);
    }
    
    /**
     * 获取统计信息
     */
    public RiskStatistics getStatistics() {
        return new RiskStatistics(
            totalChecked.sum(),
            totalApproved.sum(),
            totalRejected.sum()
        );
    }
    
    /**
     * 打印统计报告
     */
    public void printStatistics() {
        RiskStatistics stats = getStatistics();
        LOG.info("=== Risk Management Statistics ===");
        LOG.info("Total Checked: {}", stats.totalChecked);
        LOG.info("Total Approved: {}", stats.totalApproved);
        LOG.info("Total Rejected: {}", stats.totalRejected);
        if (stats.totalChecked > 0) {
            double rejectRate = (double) stats.totalRejected / stats.totalChecked * 100.0;
            LOG.info("Reject Rate: {:.2f}%", rejectRate);
        }
        LOG.info("==================================");
    }
    
    /**
     * 风控统计数据结构
     */
    public static class RiskStatistics {
        public final long totalChecked;
        public final long totalApproved;
        public final long totalRejected;
        
        public RiskStatistics(long totalChecked, long totalApproved, long totalRejected) {
            this.totalChecked = totalChecked;
            this.totalApproved = totalApproved;
            this.totalRejected = totalRejected;
        }
    }
}
