# SOR_Server
This is Smar order rooting server implemented by Java, it use ZGC and Disrupter framework which provide low latency campability.
>>>>>>> 63a01bb5b44916cabf0bee6e3a45f00b53bef9f6
# SOR_Server - Smart Order Routing Server

智能订单路由服务器，面向金融交易系统，核心功能是将客户订单智能分发至最优交易所或流动性池。

## 关键特性

- **超低延迟处理**：依托 ZGC + Disruptor
- **高吞吐订单匹配**：支持百万级 TPS
- **虚拟线程并发**：Java 21 虚拟线程替代传统线程池
- **无锁设计**：使用 VarHandle 实现 CAS 操作，避免重量级锁
- **RingBuffer 对象复用**：唯一允许的对象池模式

## 技术栈

- **JDK**: Java 21+
- **构建工具**: Maven 3.9+
- **并发框架**: LMAX Disruptor 4.0
- **网络框架**: Netty 4.1
- **日志**: SLF4J + Logback

## 模块结构

```
sor-server/
├── sor-core          # 核心路由引擎和领域模型
├── sor-disruptor     # Disruptor 事件处理框架
├── sor-network       # 网络通信层（Netty）
├── sor-marketdata    # 行情数据处理
├── sor-exchange      # 交易所网关适配器
├── sor-risk          # 风控合规模块
├── sor-monitor       # 监控指标告警
└── sor-benchmark     # JMH 性能基准测试
```

## 快速开始

### 前置条件

```bash
java -version  # >= 21
mvn -version   # >= 3.9
```

### 编译项目

```bash
mvn clean install
```

### 运行应用

```bash
cd sor-core
mvn exec:java -Dexec.mainClass="com.sor.SorApplication"
```

### JVM 参数建议

```bash
-server
-XX:+UseZGC
-XX:ZCollectionInterval=5
-XX:+ZGenerational
-Xms16g -Xmx16g
-XX:MaxDirectMemorySize=8g
```

## 核心设计

### 1. 虚拟线程使用

```java
// 使用虚拟线程处理并发请求
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var future1 = scope.fork(() -> processOrder(order1));
    var future2 = scope.fork(() -> processOrder(order2));
    scope.join();
}
```

### 2. VarHandle 无锁操作

```java
private static final VarHandle statusHandle;

public boolean compareAndSetStatus(OrderStatus expected, OrderStatus newState) {
    return statusHandle.compareAndSet(this, expected.getCode(), newState.getCode());
}
```

### 3. Disruptor 集成

```java
// RingBuffer 发布事件
ringBuffer.publishEvent((event, sequence) -> {
    event.initialize(orderId, symbol, order, EventType.NEW_ORDER);
});
```

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 端到端延迟 | < 50μs (P99) |
| 吞吐量 | > 1,000,000 TPS |
| 抖动 | < 10μs (标准差) |
| GC 停顿 | < 1ms |

## 开发规范

### 禁止事项

- ❌ 禁止使用 `ThreadPoolExecutor`、`ExecutorService` 等线程池
- ❌ 禁止使用 `synchronized`、`ReentrantLock` 等重量级锁
- ❌ 禁止使用除 RingBuffer 外的对象池
- ❌ 禁止在热路径上创建对象
- ❌ 禁止阻塞式 I/O

### 推荐实践

- ✅ 使用虚拟线程处理并发
- ✅ 使用 VarHandle 进行原子操作
- ✅ 使用 Disruptor 进行线程间通信
- ✅ 使用数组代替集合（热路径）
- ✅ 使用堆外内存存储状态

## 测试

### 单元测试

```bash
mvn test
```

### 性能基准测试

```bash
mvn clean install -Pbenchmark
cd sor-benchmark
mvn exec:java -Dexec.mainClass="org.openjdk.jmh.Main"
```

## 监控与调优

### JMX 指标

启用 JMX 监控：

```bash
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
```

### 延迟分析

使用 Async Profiler：

```bash
./profiler.sh -d 30 -f flame.html <pid>
```

## 部署

### Docker 部署

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
COPY target/sor-core-*.jar /app/sor-core.jar
ENTRYPOINT ["java", "-jar", "/app/sor-core.jar"]
```

### Kubernetes 配置

详见 `k8s/` 目录。

## 参考资料

- [Java 21 Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [LMAX Disruptor Documentation](https://lmax-exchange.github.io/disruptor/)
- [ZGC Tuning Guide](https://wiki.openjdk.org/display/zgc/Main)

## 许可证

MIT License

---

**版本**: 1.0.0-SNAPSHOT  
**构建时间**: 2026-03-26
=======
# SOR_Server
This is Smar order rooting server implemented by Java, it use ZGC and Disrupter framework which provide low latency campability.
>>>>>>> 63a01bb5b44916cabf0bee6e3a45f00b53bef9f6
