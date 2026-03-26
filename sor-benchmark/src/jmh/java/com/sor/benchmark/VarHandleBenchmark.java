package com.sor.benchmark;

import org.openjdk.jmh.annotations.*;

import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * VarHandle vs 传统锁性能对比测试
 * 
 * 测试目标：验证 VarHandle 无锁操作的性能优势
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(1)
public class VarHandleBenchmark {
    
    // VarHandle 计数器
    private static final VarHandle varHandleCounter;
    private volatile long varHandleValue = 0;
    
    // AtomicLong 计数器
    private final AtomicLong atomicValue = new AtomicLong(0);
    
    // 传统锁计数器
    private long lockValue = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            varHandleCounter = lookup.findVarHandle(VarHandleBenchmark.class, "varHandleValue", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    @Setup
    public void setup() {
        varHandleValue = 0;
        atomicValue.set(0);
        lockValue = 0;
    }
    
    /**
     * VarHandle CAS 递增
     */
    @Benchmark
    public long varHandleIncrement() {
        long oldVal, newVal;
        do {
            oldVal = (long) varHandleCounter.get(this);
            newVal = oldVal + 1;
        } while (!varHandleCounter.compareAndSet(this, oldVal, newVal));
        return newVal;
    }
    
    /**
     * AtomicLong 递增
     */
    @Benchmark
    public long atomicIncrement() {
        return atomicValue.incrementAndGet();
    }
    
    /**
     * synchronized 锁递增
     */
    @Benchmark
    public long synchronizedIncrement() {
        synchronized (this) {
            return ++lockValue;
        }
    }
    
    /**
     * ReentrantLock 递增
     */
    @Benchmark
    public long reentrantLockIncrement() {
        lock.lock();
        try {
            return ++lockValue;
        } finally {
            lock.unlock();
        }
    }
}
