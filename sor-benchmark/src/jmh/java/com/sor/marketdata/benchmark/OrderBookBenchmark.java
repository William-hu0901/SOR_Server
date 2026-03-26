package com.sor.marketdata.benchmark;

import com.sor.marketdata.OrderBook;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * OrderBook 性能基准测试
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(1)
public class OrderBookBenchmark {
    
    private OrderBook orderBook;
    
    @Setup
    public void setup() {
        orderBook = new OrderBook("AAPL", 10);
        
        // 预填充一些订单
        for (int i = 0; i < 5; i++) {
            orderBook.addBid(150.0 - i * 0.5, 100);
            orderBook.addAsk(150.5 + i * 0.5, 100);
        }
    }
    
    /**
     * 测试添加买单性能
     */
    @Benchmark
    public void testAddBid() {
        double price = 149.0 + Math.random() * 0.5;
        orderBook.addBid(price, 100);
    }
    
    /**
     * 测试添加卖单性能
     */
    @Benchmark
    public void testAddAsk() {
        double price = 151.0 + Math.random() * 0.5;
        orderBook.addAsk(price, 100);
    }
    
    /**
     * 测试获取最优买价性能
     */
    @Benchmark
    public double testGetBestBid() {
        return orderBook.getBestBid();
    }
    
    /**
     * 测试获取最优卖价性能
     */
    @Benchmark
    public double testGetBestAsk() {
        return orderBook.getBestAsk();
    }
    
    /**
     * 测试获取价差性能
     */
    @Benchmark
    public double testGetSpread() {
        return orderBook.getSpread();
    }
    
    /**
     * 测试获取中间价性能
     */
    @Benchmark
    public double testGetMidPrice() {
        return orderBook.getMidPrice();
    }
}
