# SOR_Server - Smart Order Routing Server (智能订单路由服务器)

## 项目概述

SOR_Server 是一个面向金融交易系统的智能订单路由服务器，核心功能是将客户订单智能分发至最优交易所或流动性池（Liquidity Pool）。

### 核心目标
- **超低延迟处理**：依托 ZGC 垃圾收集器和 Disruptor 无锁并发框架
- **高吞吐订单匹配**：支持每秒百万级订单处理与实时路由决策
- **微秒级时延控制**：为算法交易、高频交易等严苛时延场景设计

## 技术栈要求

### 基础环境
- **JDK 版本**: Java 21+
- **构建工具**: Maven 3.9+
- **依赖管理**: BOM (Bill of Materials)

### 核心技术组件

#### 1. 并发处理
- ✅ **虚拟线程 (Virtual Threads)**: 使用 `java.lang.Thread.startVirtualThread()` 替代传统线程池
- ✅ **VarHandle**: 使用 `java.lang.invoke.VarHandle` 实现无锁 CAS 操作，避免重量级锁（synchronized、ReentrantLock）
- ❌ **禁止使用**: ThreadPoolExecutor、ExecutorService 等传统线程池
- ⚠️ **限制使用对象池**: 仅允许使用 RingBuffer，禁止使用其他对象池（如 CommonPool、ObjectPool）

#### 2. 高性能队列
- **Disruptor**: LMAX Disruptor 无锁环形缓冲区，用于线程间高效通信
- **RingBuffer**: 作为唯一允许的对象池模式，用于事件发布/消费

#### 3. JVM 优化
- **ZGC**: 配置 ZGC 垃圾收集器，最小化 STW 停顿
- **JIT 编译优化**: 
  - 热点代码路径标记（@ForceInline、@IntrinsicCandidate）
  - 方法内联提示
  - 避免虚方法调用
  - 数据局部性优化

#### 4. 网络通信
- **Netty**: 异步事件驱动的网络框架
- **协议**: TCP/UDP 双协议栈支持
- **零拷贝**: FileRegion、CompositeByteBuf

#### 5. 数据存储
- **Off-Heap 内存**: sun.misc.Unsafe 或 MemorySegment (Java 21 Foreign Function & Data API)
- **持久化**: RocksDB（可选，用于订单日志）

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────┐
│         Client Interface Layer          │  ← REST/WebSocket/FIX 协议接入
├─────────────────────────────────────────┤
│         Protocol Adapter Layer          │  ← 协议解析与封装
├─────────────────────────────────────────┤
│         Order Processing Layer          │  ← 订单验证、风控检查
├─────────────────────────────────────────┤
│    Smart Routing Engine (Core)          │  ← 路由策略、最优价格计算
├─────────────────────────────────────────┤
│         Exchange Gateway Layer          │  ← 交易所接口适配
├─────────────────────────────────────────┤
│         Market Data Layer               │  ← 行情数据接收与处理
└─────────────────────────────────────────┘
```

### 核心模块划分

1. **sor-core**: 核心路由引擎
   - 订单路由策略
   - 最优价格计算
   - 流动性池管理

2. **sor-disruptor**: 基于 Disruptor 的事件处理
   - 订单事件生产者/消费者
   - 市场数据事件
   - 执行结果事件

3. **sor-network**: 网络通信层
   - FIX 协议编解码
   - WebSocket 处理器
   - TCP/UDP 传输

4. **sor-marketdata**: 行情数据处理
   - 实时行情订阅
   - 深度订单簿管理
   - 价差计算

5. **sor-exchange**: 交易所网关
   - 交易所适配器（NYSE、NASDAQ、CME 等）
   - 订单执行监控
   - 成交回报处理

6. **sor-risk**: 风控模块
   - 订单合法性校验
   - 仓位限制检查
   - 频率控制

7. **sor-monitor**: 监控与指标
   - 延迟统计（端到端）
   - 吞吐量监控
   - JMX 暴露

## 性能指标要求

| 指标 | 目标值 | 测量方式 |
|------|--------|----------|
| 端到端延迟 | < 50 微秒 (P99) | 订单接收到发出 |
| 吞吐量 | > 1,000,000 TPS | 订单/秒 |
| 抖动 | < 10 微秒 (标准差) | 延迟稳定性 |
| GC 停顿 | < 1ms | ZGC 配置 |
| CPU 利用率 | > 80% | 满载场景 |

## 关键实现细节

### 1. 虚拟线程使用模式

```java
// ✅ 正确用法：虚拟线程处理请求
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var future1 = scope.fork(() -> processOrder(order1));
    var future2 = scope.fork(() -> processOrder(order2));
    scope.join();
}

// ❌ 错误用法：创建线程池
ExecutorService executor = Executors.newFixedThreadPool(10); // 禁止
```

### 2. VarHandle 无锁操作

```java
// ✅ 使用 VarHandle 实现原子操作
private static final VarHandle counterHandle = 
    VarHandles.lookup().findStaticFieldHandle(
        MethodHandles.lookup(), "counter", long.class);
    
private volatile long counter;

public void increment() {
    long oldVal, newVal;
    do {
        oldVal = (long) counterHandle.get();
        newVal = oldVal + 1;
    } while (!counterHandle.compareAndSet(oldVal, newVal));
}

// ❌ 避免使用重量级锁
synchronized(lock) { ... } // 禁止
ReentrantLock.lock();      // 禁止
```

### 3. Disruptor 集成

```java
// 订单事件处理器
public class OrderEvent {
    private long orderId;
    private double price;
    private int quantity;
    // getter/setter (避免分配新对象)
}

// 事件工厂
public class OrderEventFactory implements EventFactory<OrderEvent> {
    public OrderEvent newInstance() {
        return new OrderEvent(); // 仅在初始化时创建
    }
}
```

### 4. JIT 优化提示

```java
// 强制内联
@ForceInline
private static int calculatePrice(double base, double factor) {
    return (int)(base * factor);
}

// 热点方法提示
@HotSpotIntrinsicCandidate
public static void arrayCopy(Object src, Object dest) {
    System.arraycopy(src, 0, dest, 0, length);
}
```

### 5. 堆外内存使用

```java
// Java 21 Foreign Function & Data API
MemorySegment offHeap = Arena.global().allocate(1024);
offHeap.set(ValueLayout.JAVA_LONG, 0, value);
```

## 项目约束

### 编码规范
1. **包命名**: `com.sor.{module}.domain|handler|util|config`
2. **日志**: 使用 SLF4J + Logback，生产环境禁止 DEBUG 级别
3. **异常处理**: 使用快速失败策略，避免捕获 Exception
4. **代码风格**: 遵循 Google Java Style

### 性能红线
1. ❌ 禁止在热路径上创建对象（尤其是集合类）
2. ❌ 禁止使用同步原语（synchronized、Lock）
3. ❌ 禁止阻塞式 I/O（使用 Netty 异步 I/O）
4. ❌ 禁止反射调用（使用代码生成或方法句柄）
5. ❌ 禁止序列化/反序列化（使用二进制协议）

### 测试要求
1. **单元测试**: JUnit 5 + Mockito，覆盖率 > 80%
2. **性能测试**: JMH 基准测试
3. **压力测试**: Gatling 负载测试
4. **延迟分析**: Async Profiler 火焰图

## 部署配置

### JVM 参数示例

```bash
-server
-XX:+UseZGC
-XX:ZCollectionInterval=5
-XX:ConcGCThreads=2
-XX:+ZGenerational
-Xms16g -Xmx16g
-XX:MaxDirectMemorySize=8g
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
--enable-preview  # 如需预览特性
```

### 硬件建议
- **CPU**: 高频率多核（>= 4.0GHz, >= 16 核）
- **内存**: DDR4/DDR5 ECC，低延迟模式
- **网络**: 10GbE+，支持 DPDK
- **存储**: NVMe SSD（用于持久化）

## 里程碑

### Phase 1: 基础框架搭建 (Week 1-2)
- [x] Maven 项目结构
- [ ] Disruptor 集成
- [ ] 虚拟线程封装
- [ ] 基础数据结构

### Phase 2: 核心功能实现 (Week 3-6)
- [ ] 订单路由引擎
- [ ] 市场数据处理
- [ ] 交易所网关
- [ ] 风控模块

### Phase 3: 性能优化 (Week 7-8)
- [ ] JIT 优化
- [ ] 内存布局优化
- [ ] GC 调优
- [ ] 延迟压测

### Phase 4: 生产就绪 (Week 9-10)
- [ ] 监控告警
- [ ] 文档完善
- [ ] 灰度部署
- [ ] 性能验收

## 参考资料

- [Java 21 Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/)
- [ZGC Tuning Guide](https://wiki.openjdk.org/display/zgc/Main)
- [FIX Protocol](https://www.fixtrading.org/standards/)

---

**版本**: 1.0  
**最后更新**: 2026-03-26  
**维护者**: SOR Team
