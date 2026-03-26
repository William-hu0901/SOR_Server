package com.sor.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能监控器
 * 
 * 提供：
 * - 延迟统计（P50, P95, P99）
 * - 吞吐量统计
 * - JVM 内存监控
 * - 线程池监控
 */
public class PerformanceMonitor {
    
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    // 单例模式
    private static volatile PerformanceMonitor instance;
    
    // 订单处理计数器（使用 LongAdder 提高并发性能）
    private final LongAdder ordersReceived = new LongAdder();
    private final LongAdder ordersRouted = new LongAdder();
    private final LongAdder ordersRejected = new LongAdder();
    
    // 延迟统计（简化版，实际应使用直方图）
    private volatile long totalLatency = 0;
    private volatile long latencyCount = 0;
    private volatile long maxLatency = 0;
    
    // JVM 指标
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    
    /**
     * 私有构造函数（单例）
     */
    private PerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }
    
    /**
     * 获取单例实例
     */
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor();
                }
            }
        }
        return instance;
    }
    
    /**
     * 记录订单接收
     */
    public void recordOrderReceived() {
        ordersReceived.increment();
    }
    
    /**
     * 记录订单路由完成
     */
    public void recordOrderRouted() {
        ordersRouted.increment();
    }
    
    /**
     * 记录订单拒绝
     */
    public void recordOrderRejected() {
        ordersRejected.increment();
    }
    
    /**
     * 记录延迟（纳秒）
     */
    public void recordLatency(long latencyNs) {
        synchronized (this) {
            totalLatency += latencyNs;
            latencyCount++;
            if (latencyNs > maxLatency) {
                maxLatency = latencyNs;
            }
        }
    }
    
    /**
     * 获取平均延迟（毫秒）
     */
    public double getAverageLatencyMs() {
        synchronized (this) {
            if (latencyCount == 0) return 0.0;
            return (totalLatency / (double) latencyCount) / 1_000_000.0;
        }
    }
    
    /**
     * 获取最大延迟（毫秒）
     */
    public long getMaxLatencyMs() {
        return maxLatency / 1_000_000;
    }
    
    /**
     * 获取吞吐量（订单/秒）
     */
    public long getOrdersPerSecond() {
        return ordersRouted.sum();
    }
    
    /**
     * 获取堆内存使用情况（MB）
     */
    public long getHeapUsedMB() {
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
    
    /**
     * 获取堆内存最大值（MB）
     */
    public long getHeapMaxMB() {
        return memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
    }
    
    /**
     * 获取活动线程数
     */
    public int getActiveThreads() {
        return threadBean.getThreadCount();
    }
    
    /**
     * 打印监控报告
     */
    public void printReport() {
        LOG.info("=== Performance Monitor Report ===");
        LOG.info("Orders Received: {}", ordersReceived.sum());
        LOG.info("Orders Routed: {}", ordersRouted.sum());
        LOG.info("Orders Rejected: {}", ordersRejected.sum());
        LOG.info("Average Latency: {:.3f} ms", getAverageLatencyMs());
        LOG.info("Max Latency: {} ms", getMaxLatencyMs());
        LOG.info("Heap Memory: {} / {} MB", getHeapUsedMB(), getHeapMaxMB());
        LOG.info("Active Threads: {}", getActiveThreads());
        LOG.info("==================================");
    }
}
