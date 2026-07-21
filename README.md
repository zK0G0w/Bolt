# Bolt

基于 JDK 25 构建的轻量级 RTB（Real-Time Bidding）广告竞价引擎，聚焦核心竞价链路实现。

## 技术栈

- JDK 25（Virtual Threads / ScopedValue / Sealed Types / Record / Pattern Matching）
- Spring Boot 3.5 + WebMVC（开启 Virtual Threads）
- OkHttp 4.x（DSP 请求 HTTP 客户端）
- Caffeine 3.x + Redis Pub/Sub（本地缓存 + 配置热更新）
- Spring Data Redis + Lettuce（Redis 客户端）

## 核心链路

```
POST /bid → ScopedValue 绑定请求上下文
  → DspFanOutService (Virtual Threads + invokeAll 并发扇出)
    → DspClient.bid() × N  (并发请求多个 DSP)
  → AuctionService (一价竞价决策，Pattern Matching 分发)
  → 响应组装 BidResponse + 追踪 URL 生成（AES 加密）
```

## 架构特性

| 模块 | 实现 | JDK 25 特性 |
|------|------|-------------|
| 并发扇出 | 每请求 VT Executor + invokeAll 统一超时 | Virtual Threads |
| 请求上下文 | ScopedValue 替代 ThreadLocal | ScopedValue |
| DSP 响应建模 | Sealed Interface (Success/NoBid/Timeout/Error) | Sealed Types + Record |
| 竞价决策 | switch 表达式 + 类型模式匹配 | Pattern Matching |
| 配置中心 | Redis 存储 + Caffeine 本地缓存 + Pub/Sub 实时失效 | — |
| 追踪埋点 | AES 加密参数 + 时间戳防重放 | — |

## 性能基准

测试环境：MacBook Pro M3 Max / 64GB RAM / JDK 25 / Redis 7.x 本地单机

测试工具：[hey](https://github.com/rakyll/hey)，MockDspClient 模拟 30-120ms 随机延迟

| 并发连接 | QPS | p50 | p99 | 说明 |
|---------|-----|-----|-----|------|
| 200 | 2,127 | 97ms | 123ms | 轻负载 |
| 1000 | **10,260** | 100ms | 140ms | Virtual Threads 线性扩展 |

> 平均响应时间几乎不随并发数增长（稳定在 ~100ms），QPS 与并发数呈线性关系。这是 Virtual Threads 的优势——没有线程池上限，并发连接从 200 扩到 1000，延迟不退化，吞吐翻 5 倍。

## 快速启动

```bash
# 前置：JDK 25 + Redis

# 启动（dev profile 自动播种样本数据到 Redis）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 发起竞价请求
curl -X POST http://localhost:9292/bid \
  -H "Content-Type: application/json" \
  -d '{"id":"req-001","imp":{"id":"imp-001","format":{"type":"native_feed","imageCount":1,"titleLen":30,"textLen":50},"dealType":{"type":"rtb","bidFloor":200},"bidFloor":200,"width":640,"height":320},"app":{"id":"app-001","name":"TestApp","packageName":"com.test","version":"1.0"},"device":{"id":{"oaid":"oaid-123"},"hardware":{"type":"PHONE","make":"Xiaomi","model":"14","os":"ANDROID","osVersion":"15","screenWidth":1080,"screenHeight":2400,"connection":"WIFI","carrier":"CHINA_MOBILE"},"geo":{"lon":116.4,"lat":39.9,"ip":"10.0.0.1","country":"CN","region":"BJ"}},"user":{"id":"user-001","interests":["game"]},"tmax":500}'

# 模拟配置热更新
redis-cli -a redis123456 -n 10 PUBLISH bolt:cache:invalidate '{"entity":"dsp","id":"plat-001","action":"update"}'
```

## 项目目标

以真实 RTB 场景为载体，实践 JDK 25 的现代语言特性与并发模型，对比传统线程池方案在代码复杂度和资源消耗上的差异。

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=zK0G0w/Bolt&type=Date)](https://star-history.com/#zK0G0w/Bolt&Date)