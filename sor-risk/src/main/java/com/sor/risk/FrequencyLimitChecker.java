package com.sor.risk;

import com.sor.core.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 频率限制检查器
 * 
 * 限制单位时间内的订单提交频率（防抖和防滥用）
 */
public class FrequencyLimitChecker implements RiskChecker {
    
    private static final Logger LOG = LoggerFactory.getLogger(FrequencyLimitChecker.class);
    
    // 每个交易者的订单计数器
    private final ConcurrentHashMap<String, LongAdder> traderCounters;
    
    // 每个交易品种的全局计数器
    private final ConcurrentHashMap<String, LongAdder> symbolCounters;
    
    // 全局总计数器
    private final LongAdder globalCounter;
    
    // 每秒最大订单数（单个交易者）
    private final int maxOrdersPerTraderPerSecond;
    
    // 每秒最大订单数（单个交易品种）
    private final int maxOrdersPerSymbolPerSecond;
    
    // 全局每秒最大订单数
    private final int maxGlobalOrdersPerSecond;
    
    // 上次重置时间（秒）
    private volatile long lastResetTime;
    
    /**
     * 构造函数
     */
    public FrequencyLimitChecker(int maxTraderFreq, int maxSymbolFreq, int maxGlobalFreq) {
        this.traderCounters = new ConcurrentHashMap<>();
        this.symbolCounters = new ConcurrentHashMap<>();
        this.globalCounter = new LongAdder();
        this.maxOrdersPerTraderPerSecond = maxTraderFreq;
        this.maxOrdersPerSymbolPerSecond = maxSymbolFreq;
        this.maxGlobalOrdersPerSecond = maxGlobalFreq;
        this.lastResetTime = System.currentTimeMillis() / 1000;
    }
    
    @Override
    public RiskCheckResult check(Order order) {
        // 检查是否需要重置计数器（每秒重置）
        resetIfNeeded();
        
        // 获取交易者 ID（简化版，实际应从订单中获取）
        String traderId = getTraderId(order);
        
        // 检查交易者频率
        LongAdder traderCounter = traderCounters.computeIfAbsent(traderId, k -> new LongAdder());
        synchronized (traderCounter) {
            long traderCount = traderCounter.sum();
            if (traderCount >= maxOrdersPerTraderPerSecond) {
                LOG.warn("Trader frequency limit exceeded: trader={}, count={}", 
                        traderId, traderCount);
                return RiskCheckResult.fail(
                    String.format("Trader frequency %d exceeds limit %d/sec", 
                                traderCount, maxOrdersPerTraderPerSecond));
            }
            traderCounter.increment();
        }
        
        // 检查交易品种频率
        LongAdder symbolCounter = symbolCounters.computeIfAbsent(order.getSymbol(), k -> new LongAdder());
        synchronized (symbolCounter) {
            long symbolCount = symbolCounter.sum();
            if (symbolCount >= maxOrdersPerSymbolPerSecond) {
                LOG.warn("Symbol frequency limit exceeded: symbol={}, count={}", 
                        order.getSymbol(), symbolCount);
                return RiskCheckResult.fail(
                    String.format("Symbol frequency %d exceeds limit %d/sec", 
                                symbolCount, maxOrdersPerSymbolPerSecond));
            }
            symbolCounter.increment();
        }
        
        // 检查全局频率
        synchronized (globalCounter) {
            long globalCount = globalCounter.sum();
            if (globalCount >= maxGlobalOrdersPerSecond) {
                LOG.warn("Global frequency limit exceeded: count={}", globalCount);
                return RiskCheckResult.fail(
                    String.format("Global frequency %d exceeds limit %d/sec", 
                                globalCount, maxGlobalOrdersPerSecond));
            }
            globalCounter.increment();
        }
        
        LOG.trace("Frequency check passed: trader={}, symbol={}", traderId, order.getSymbol());
        return RiskCheckResult.success();
    }
    
    /**
     * 重置计数器（如果需要）
     */
    private void resetIfNeeded() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime > lastResetTime) {
            // 使用双重检查锁定模式确保只有一个线程执行重置
            if (lastResetTime < currentTime) {
                synchronized (this) {
                    if (lastResetTime < currentTime) {
                        // 清空所有计数器
                        traderCounters.values().forEach(LongAdder::reset);
                        symbolCounters.values().forEach(LongAdder::reset);
                        globalCounter.reset();
                        lastResetTime = currentTime;
                        LOG.debug("Frequency counters reset");
                    }
                }
            }
        }
    }
    
    /**
     * 获取交易者 ID（简化实现）
     */

    private String getTraderId(Order order) {
        // 实际应用中应该从订单的账户字段获取
        //return "TRADER_" + Math.abs(order.getOrderId() % 1000);
        return String.valueOf(order.getOrderId());
    }
}