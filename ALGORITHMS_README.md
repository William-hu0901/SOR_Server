# SOR 核心算法库文档

## 概述

本文档介绍了 SOR_Server 项目中实现的 6 个核心交易算法，这些算法位于 `sor-core` 模块的 `com.sor.core.algorithm` 包中。

---

## 算法列表

### 1. 订单匹配算法（OrderMatchingAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/OrderMatchingAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/OrderMatchingAlgorithm.java)

**功能描述**:
- 实现经典的限价订单簿匹配逻辑
- 遵循"价格优先、时间优先"原则
- 支持多价格水平匹配和部分成交

**核心特性**:
- ✅ 价格优先：较高买价/较低卖价优先
- ✅ 时间优先：同价位订单按时间顺序
- ✅ 数量匹配：成交量取两者较小值
- ✅ 价格确定：成交价取被动方价格

**测试类**: [`OrderMatchingAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/OrderMatchingAlgorithmTest.java)

**使用示例**:
```java
OrderMatchingAlgorithm matcher = new OrderMatchingAlgorithm();

// 创建卖单簿
List<OrderMatchingAlgorithm.PriceLevel> sellBook = new ArrayList<>();
OrderMatchingAlgorithm.PriceLevel level = new OrderMatchingAlgorithm.PriceLevel(100.0);
level.addOrder(new TestOrder(2L, 100.0, 500, Side.SELL, nanoTime));
sellBook.add(level);

// 匹配买单
TestOrder buyOrder = new TestOrder(1L, 100.0, 300, Side.BUY, nanoTime);
MatchResult result = matcher.matchOrder(buyOrder, sellBook);
```

---

### 2. VWAP 算法（VWAPAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/VWAPAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/VWAPAlgorithm.java)

**功能描述**:
- 计算成交量加权平均价格（Volume Weighted Average Price）
- 支持批量计算和增量计算
- 提供执行质量评估功能

**计算公式**:
```
VWAP = Σ(Price × Volume) / Σ(Volume)
```

**核心特性**:
- ✅ 批量 VWAP 计算
- ✅ 增量式 VWAP 计算器
- ✅ 滑动窗口 VWAP
- ✅ 时间范围 VWAP
- ✅ 偏离度分析
- ✅ 执行质量评估

**测试类**: [`VWAPAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/VWAPAlgorithmTest.java)

**使用示例**:
```java
VWAPAlgorithm vwap = new VWAPAlgorithm();

// 批量计算
List<VWAPAlgorithm.Trade> trades = new ArrayList<>();
trades.add(new Trade(100.0, 1000, timestamp));
trades.add(new Trade(101.0, 2000, timestamp + 1000));

VWAPResult result = vwap.calculate(trades);
double vwapValue = result.getVWAP();  // 100.25
```

---

### 3. TWAP 算法（TWAPAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/TWAPAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/TWAPAlgorithm.java)

**功能描述**:
- 时间加权平均价格策略（Time Weighted Average Price）
- 将大额订单在时间维度上均匀分割
- 减少市场冲击，隐藏交易意图

**核心思想**:
```
1. 将总订单分成 N 份
2. 在固定时间间隔 T 内执行一份
3. 最终成交价即为 TWAP
```

**分割策略**:
- ✅ 等分策略（EQUAL）
- ✅ 比例策略（PROPORTIONAL）
- ✅ 随机策略（RANDOM）
- ✅ 自适应策略（ADAPTIVE）

**测试类**: [`TWAPAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/TWAPAlgorithmTest.java)

**使用示例**:
```java
TWAPConfig config = new TWAPConfig(
    10000,   // 总数量
    10,      // 分 10 份
    60_000_000_000L  // 每分钟执行一次
);

TWAPScheduler scheduler = new TWAPScheduler(config);
List<TWAPSlice> slices = scheduler.generateSchedule(startTime);
```

---

### 4. 流动性聚合算法（LiquidityAggregationAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/LiquidityAggregationAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/LiquidityAggregationAlgorithm.java)

**功能描述**:
- 将多个交易所/流动性提供商的订单簿合并为虚拟订单簿
- 智能路由到最优执行路径
- 考虑费用、延迟等因素

**核心目标**:
- ✅ 深度最大化
- ✅ 价格优化
- ✅ 延迟最小化
- ✅ 成本控制

**聚合策略**:
- ✅ 价格优先聚合
- ✅ 加权聚合
- ✅ 分层聚合

**测试类**: [`LiquidityAggregationAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/LiquidityAggregationAlgorithmTest.java)

**使用示例**:
```java
LiquidityAggregationAlgorithm aggregator = new LiquidityAggregationAlgorithm();

// 聚合来自不同 LP 的订单
List<OrderItem> orders = new ArrayList<>();
orders.add(new OrderItem("LP1", 99.5, 1000, Side.BID, timestamp));
orders.add(new OrderItem("LP2", 99.6, 1500, Side.BID, timestamp));

AggregatedBook book = aggregator.aggregate(orders);
double bestBid = book.getBestBid();  // 99.6
```

---

### 5. 价格发现算法（PriceDiscoveryAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/PriceDiscoveryAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/PriceDiscoveryAlgorithm.java)

**功能描述**:
- 基于订单簿计算资产公允价值
- 检测价格偏离和套利机会
- 识别支撑位和阻力位

**计算方法**:
```
Fair Value = Σ(Price_i × Weight_i) / Σ(Weight_i)
Weight_i = Quantity_i / Distance_from_mid
```

**核心功能**:
- ✅ 公允价值计算（三重加权）
- ✅ 买卖不平衡度分析
- ✅ 置信度评估
- ✅ 支撑/阻力位识别
- ✅ 套利机会检测

**测试类**: [`PriceDiscoveryAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/PriceDiscoveryAlgorithmTest.java)

**使用示例**:
```java
PriceDiscoveryAlgorithm priceDiscovery = new PriceDiscoveryAlgorithm();

List<OrderBookItem> bids = Arrays.asList(
    new OrderBookItem(99.5, 1000, Side.BID),
    new OrderBookItem(99.4, 2000, Side.BID)
);

List<OrderBookItem> asks = Arrays.asList(
    new OrderBookItem(100.5, 1000, Side.ASK),
    new OrderBookItem(100.6, 2000, Side.ASK)
);

PriceDiscoveryResult result = priceDiscovery.discoverPrice(bids, asks);
double fairValue = result.getFairValue();  // ~100.0
```

---

### 6. 订单分割算法（OrderSlicingAlgorithm）

**文件位置**: [`sor-core/src/main/java/com/sor/core/algorithm/OrderSlicingAlgorithm.java`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/main/java/com/sor/core/algorithm/OrderSlicingAlgorithm.java)

**功能描述**:
- 将大额订单分解为多个小额订单
- 减少市场冲击成本
- 隐藏交易意图

**数学模型**:
```
最优分割数 N* = sqrt(总数量 / 市场深度)
单次执行量 = 总数量 / N*
冲击成本 ∝ (执行量 / 市场深度)^2
```

**分割策略**:
- ✅ 等分策略（EQUAL）
- ✅ 比例策略（PROPORTIONAL）
- ✅ 随机策略（RANDOM）
- ✅ 自适应策略（ADAPTIVE）

**测试类**: [`OrderSlicingAlgorithmTest`](file:///C:/Users/delon/IdeaProjects/SOR_Server/sor-core/src/test/java/com/sor/core/algorithm/OrderSlicingAlgorithmTest.java)

**使用示例**:
```java
OrderSlicingAlgorithm slicer = new OrderSlicingAlgorithm();

SlicingConfig config = new SlicingConfig(
    10000,   // 总数量
    0.02,    // 最大冲击 2%
    100,     // 最小切片
    2000,    // 最大切片
    SlicingStrategy.EQUAL,
    false,   // 不随机化
    60       // 1 小时完成
);

MarketDepth depth = new MarketDepth(5000, 5000, 0.01, 0.02);
SlicingResult result = slicer.sliceOrder(config, depth);
```

---

## 测试总结

### 测试覆盖率

| 算法 | 测试类 | 测试方法数 | 通过率 |
|------|--------|-----------|--------|
| OrderMatching | OrderMatchingAlgorithmTest | 3 | 100% ✅ |
| VWAP | VWAPAlgorithmTest | 3 | 100% ✅ |
| TWAP | TWAPAlgorithmTest | 3 | 100% ✅ |
| LiquidityAggregation | LiquidityAggregationAlgorithmTest | 2 | 100% ✅ |
| PriceDiscovery | PriceDiscoveryAlgorithmTest | 3 | 100% ✅ |
| OrderSlicing | OrderSlicingAlgorithmTest | 3 | 100% ✅ |
| **总计** | **6** | **17** | **100% ✅** |

### 运行测试

```bash
cd C:\Users\delon\IdeaProjects\SOR_Server
mvn test -pl sor-core
```

测试结果：
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 性能特征

### 时间复杂度

| 算法 | 时间复杂度 | 空间复杂度 | 适用场景 |
|------|-----------|-----------|---------|
| OrderMatching | O(n) | O(1) | 实时订单撮合 |
| VWAP | O(n) | O(1) | 历史数据分析 |
| TWAP | O(n) | O(n) | 订单执行计划 |
| LiquidityAggregation | O(m×n) | O(m) | 多市场集成 |
| PriceDiscovery | O(n) | O(n) | 公允价值评估 |
| OrderSlicing | O(n) | O(n) | 大单执行 |

### 优化技术

- ✅ 使用流式 API 简化集合操作
- ✅ 预分配容量减少对象创建
- ✅ 增量计算避免全量重算
- ✅ TreeMap 自动排序
- ✅ ConcurrentHashMap 线程安全

---

## 实际应用场景

### 智能订单路由流程

1. **价格发现** → 使用 `PriceDiscoveryAlgorithm` 计算公允价值
2. **流动性聚合** → 使用 `LiquidityAggregationAlgorithm` 整合多个市场
3. **订单分割** → 使用 `OrderSlicingAlgorithm` 拆分大单
4. **执行监控** → 使用 `VWAPAlgorithm` 评估执行质量
5. **订单匹配** → 使用 `OrderMatchingAlgorithm` 撮合成交

### 算法交易策略

- **VWAP 策略**：跟踪成交量加权平均价格
- **TWAP 策略**：时间加权平均价格执行
- **冰山订单**：使用订单分割隐藏真实数量
- **统计套利**：基于价格发现的套利机会

---

## 扩展建议

### 未来可以添加的算法

1. **实施短差算法（Implementation Shortfall）**
   - 平衡执行速度和价格风险
   
2. **狙击手算法（Sniper）**
   - 快速捕捉短暂的市场机会
   
3. **冰山算法（Iceberg）**
   - 隐藏真实订单数量
   
4. **挂钩算法（Pegging）**
   - 订单价格跟随市场动态调整

5. **做市商算法（Market Making）**
   - 双边报价提供流动性

---

## 参考资料

- [Almgren-Chriss Model](https://en.wikipedia.org/wiki/Almgren%E2%80%93Chriss_model) - 最优执行理论
- [VWAP Trading Strategy](https://www.investopedia.com/terms/v/vwap.asp) - 机构常用策略
- [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/) - 高性能订单匹配引擎

---

**版本**: 1.0  
**创建时间**: 2026-03-26  
**作者**: SOR Team  
**测试状态**: ✅ 全部通过（17/17）
