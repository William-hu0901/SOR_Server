package com.sor;

import com.sor.core.domain.Order;
import com.sor.core.domain.OrderSide;
import com.sor.core.domain.OrderType;
import com.sor.core.routing.LiquidityManager;
import com.sor.disruptor.DisruptorConfig;
import com.sor.network.TcpServer;
import com.sor.monitor.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SOR Server 主应用程序入口
 */
public class SorApplication {
    
    private static final Logger LOG = LoggerFactory.getLogger(SorApplication.class);
    
    // 核心组件
    private final LiquidityManager liquidityManager;
    private final DisruptorConfig disruptorConfig;
    private final TcpServer tcpServer;
    private final PerformanceMonitor monitor;
    
    // 服务端口
    private static final int FIX_PORT = 9876;
    
    /**
     * 构造函数
     */
    public SorApplication() {
        LOG.info("Initializing SOR Server...");
        
        // 初始化流动性管理器
        this.liquidityManager = new LiquidityManager();
        
        // 初始化 Disruptor
        this.disruptorConfig = new DisruptorConfig();
        
        // 初始化 TCP 服务器
        this.tcpServer = new TcpServer(FIX_PORT);
        
        // 获取性能监控器实例
        this.monitor = PerformanceMonitor.getInstance();
        
        LOG.info("SOR Server initialized successfully");
    }
    
    /**
     * 启动服务
     */
    public void start() {
        LOG.info("Starting SOR Server...");
        
        // 初始化模拟流动性数据
        liquidityManager.initMockLiquidity("AAPL");
        liquidityManager.initMockLiquidity("GOOG");
        liquidityManager.initMockLiquidity("MSFT");
        
        // 启动 TCP 服务器
        try {
            tcpServer.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Failed to start TCP server", e);
        }
        
        // 打印初始报告
        monitor.printReport();
        
        LOG.info("SOR Server started successfully");
        LOG.info("Listening for orders on port {}", FIX_PORT);
    }
    
    /**
     * 停止服务
     */
    public void stop() {
        LOG.info("Stopping SOR Server...");
        
        // 打印最终报告
        monitor.printReport();
        
        // 关闭 TCP 服务器
        tcpServer.stop();
        
        // 关闭 Disruptor
        disruptorConfig.shutdown();
        
        LOG.info("SOR Server stopped");
    }
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        LOG.info("=== SOR Server Starting ===");
        LOG.info("Java Version: {}", System.getProperty("java.version"));
        LOG.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        LOG.info("Max Heap Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        
        // 创建应用实例
        SorApplication app = new SorApplication();
        
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook triggered");
            app.stop();
        }, "SOR-Shutdown-Hook"));
        
        // 启动服务
        app.start();
        
        // 测试示例（实际生产中应该移除）
        testOrders(app);
        
        // 保持运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 测试订单（演示用）
     */
    private static void testOrders(SorApplication app) {
        // 使用虚拟线程提交测试订单
        Thread.startVirtualThread(() -> {
            for (int i = 0; i < 10; i++) {
                Order order = new Order(
                    System.nanoTime() + i,
                    "AAPL",
                    OrderSide.BUY,
                    OrderType.LIMIT,
                    100,
                    150.0 + i * 0.1,
                    System.nanoTime()
                );
                
                // 记录监控指标
                app.monitor.recordOrderReceived();
                
                long startTime = System.nanoTime();
                
                // TODO: 提交到 Disruptor 处理
                LOG.info("Test order created: {}", order);
                
                long latency = System.nanoTime() - startTime;
                app.monitor.recordLatency(latency);
                app.monitor.recordOrderRouted();
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            LOG.info("Test orders submitted");
            
            // 打印监控报告
            app.monitor.printReport();
        });
    }
}
