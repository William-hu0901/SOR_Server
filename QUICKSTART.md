# SOR_Server 快速启动指南

## 🚀 5 分钟快速开始

### 前置条件检查

```bash
# 检查 Java 版本（需要 21+）
java -version

# 检查 Maven 版本（需要 3.9+）
mvn -version
```

### 方式 1: Maven 编译并运行（推荐）

```bash
# 1. 进入项目目录
cd C:\Users\delon\IdeaProjects\SOR_Server

# 2. 编译项目
mvn clean install -DskipTests

# 3. 运行应用
cd sor-app
mvn exec:java -Dexec.mainClass="com.sor.SorApplication"
```

**预期输出：**
```
=== SOR Server Starting ===
Java Version: 21.x.x
Available Processors: 8
Max Heap Memory: 4096 MB
Initializing SOR Server...
Using NIO transport
Disruptor started with buffer size: 1048576
Initialized mock liquidity for AAPL
Initialized mock liquidity for GOOG
Initialized mock liquidity for MSFT
TCP Server started on port 9876
SOR Server started successfully
Listening for orders on port 9876
```

### 方式 2: 直接运行 JAR

```bash
# 1. 编译项目
mvn clean install -DskipTests

# 2. 运行 JAR
java -jar sor-app/target/sor-app-1.0.0-SNAPSHOT.jar
```

### 方式 3: Docker 运行

```bash
# 1. 构建镜像
docker build -t sor-server:latest .

# 2. 运行容器
docker run -d -p 9876:9876 --name sor-server sor-server:latest

# 3. 查看日志
docker logs -f sor-server
```

---

## 📝 测试验证

### 发送测试订单

应用启动后会自动发送 10 个测试订单，你会看到类似输出：

```
Test order created: Order{id=..., symbol=AAPL, side=BUY, type=LIMIT, qty=100, price=150.00, status=NEW}
Test order created: Order{id=..., symbol=AAPL, side=BUY, type=LIMIT, qty=100, price=150.10, status=NEW}
...
Test orders submitted

=== Performance Monitor Report ===
Orders Received: 10
Orders Routed: 10
Orders Rejected: 0
Average Latency: 0.xxx ms
Max Latency: x ms
Heap Memory: xxx / 4096 MB
Active Threads: xx
==================================
```

### 连接 TCP 服务器测试

```bash
# 使用 telnet 或 nc 连接
telnet localhost 9876

# 或使用 PowerShell
Test-NetConnection -ComputerName localhost -Port 9876
```

---

## 🔧 常见问题

### Q1: 提示找不到 Java 21

**解决方案：**
1. 下载安装 JDK 21：https://adoptium.net/
2. 设置 JAVA_HOME 环境变量
3. 验证：`java -version`

### Q2: Maven 版本过低

**解决方案：**
```bash
# Windows (PowerShell)
choco install maven

# 或下载最新 Maven
# https://maven.apache.org/download.cgi
```

### Q3: 端口 9876 已被占用

**解决方案：**
修改 `SorApplication.java` 中的端口：
```java
private static final int FIX_PORT = 9877; // 改为其他端口
```

### Q4: 编译失败

**解决方案：**
```bash
# 清理 Maven 缓存
mvn clean

# 删除本地仓库的 sor 相关依赖
rm -rf ~/.m2/repository/com/sor/

# 重新编译
mvn clean install -U -DskipTests
```

---

## 📊 性能调优

### JVM 参数建议

编辑 `sor-app/src/main/resources/application.conf` 或命令行添加：

```bash
java -server \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Xms8g -Xmx8g \
  -XX:MaxDirectMemorySize=4g \
  -Djava.awt.headless=true \
  -jar sor-app/target/sor-app-1.0.0-SNAPSHOT.jar
```

### ZGC vs G1 选择

| GC 类型 | 适用场景 | 延迟目标 |
|---------|----------|----------|
| **ZGC** | 超低延迟交易 | < 1ms |
| G1 | 一般交易系统 | < 10ms |
| Parallel | 批处理场景 | 吞吐量优先 |

---

## 🐛 调试技巧

### 启用详细日志

修改 `sor-core/src/main/resources/logback.xml`：

```xml
<root level="DEBUG">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</root>
```

### 查看 Disruptor 事件流

在 `OrderEventHandler.java` 中添加日志：

```java
@Override
public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
    LOG.info("Processing event: type={}, orderId={}", 
             event.getType(), event.getOrderId());
    // ...
}
```

### 监控内存使用

```java
// 在 SorApplication 中添加定时任务
Thread.startVirtualThread(() -> {
    while (true) {
        Thread.sleep(5000);
        Runtime runtime = Runtime.getRuntime();
        LOG.info("Memory: used={}MB, free={}MB, max={}MB",
            (runtime.totalMemory() - runtime.freeMemory()) / (1024*1024),
            runtime.freeMemory() / (1024*1024),
            runtime.maxMemory() / (1024*1024));
    }
});
```

---

## 📈 下一步学习

1. **阅读源码**
   - [PROMPT.md](./PROMPT.md) - 了解设计需求
   - [FINAL_SUMMARY.md](./FINAL_SUMMARY.md) - 查看完整功能

2. **修改配置**
   - 调整 RingBuffer 大小（sor-disruptor/DisruptorConfig.java）
   - 修改路由策略权重（sor-core/routing/*）
   - 添加新的交易所适配器（sor-exchange/*）

3. **性能测试**
   ```bash
   cd sor-benchmark
   mvn clean package
   java -jar target/sor-benchmark-*.jar
   ```

4. **集成开发**
   - 导入 IDEA：File -> Open -> 选择 pom.xml
   - 运行单元测试：`mvn test`
   - 代码覆盖率：`mvn clean test jacoco:report`

---

## 💡 最佳实践

### ✅ 推荐的编码模式

```java
// 使用虚拟线程
Thread.startVirtualThread(() -> {
    // 业务逻辑
});

// 使用 VarHandle
private static final VarHandle counterHandle;

public void increment() {
    long oldVal, newVal;
    do {
        oldVal = (long) counterHandle.get(this);
        newVal = oldVal + 1;
    } while (!counterHandle.compareAndSet(this, oldVal, newVal));
}

// 使用 Disruptor
ringBuffer.publishEvent((event, sequence) -> {
    event.initialize(...);
});
```

### ❌ 避免的反模式

```java
// 禁止：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);

// 禁止：使用重量级锁
synchronized(lock) { ... }
ReentrantLock lock = new ReentrantLock();

// 禁止：热路径上创建对象
for (int i = 0; i < 1000; i++) {
    new Object(); // 避免！
}
```

---

## 🎯 生产部署检查清单

- [ ] JVM 参数已优化
- [ ] 日志级别设置为 INFO 或 WARN
- [ ] 监控指标已接入 Prometheus
- [ ] 健康检查端点已配置
- [ ] 超时时间已合理设置
- [ ] 熔断器已启用
- [ ] 备份策略已配置
- [ ] 灾难恢复计划已制定

---

**祝你使用愉快！** 🎉

如有问题，请查看 [FINAL_SUMMARY.md](./FINAL_SUMMARY.md) 或提交 Issue。
