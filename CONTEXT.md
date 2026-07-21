# Bolt — 项目上下文

## 定位

基于 JDK 25 的轻量级 RTB 竞价引擎，个人学习项目。以真实 RTB 场景为载体，实践现代 Java 特性。

## 核心链路

```
BidController (POST /bid)
  → ScopedValue.runWhere 绑定 BidContext
    → BidService.process()
      → DspFanOutService.fanOut()          // Virtual Threads + invokeAll
        → DspClient.bid() × N             // 并发请求多个 DSP
      → AuctionService.runAuction()        // 一价竞价决策
      → 响应组装 BidResponse
```

## 关键设计决策

| 决策 | 选择 | 理由 | ADR |
|------|------|------|-----|
| DSP 响应建模 | Sealed Interface + Record | 穷举终态，不同子类型携带不同字段 | [001](docs/adr/001-sealed-dsp-bid-result.md) |
| 请求上下文传递 | ScopedValue | Virtual Thread 友好，不可变，生命周期明确 | [002](docs/adr/002-scoped-value-context.md) |
| 并发扇出 | invokeAll + 每请求新建 VT executor | VT 极轻量无需池化，invokeAll 统一超时语义 | [003](docs/adr/003-virtual-thread-fan-out.md) |
| BidRequest 结构 | 单 Imp（非 List） | 简化扇出和竞价逻辑，学习项目够用 | — |
| 竞价模式 | 一价拍卖 | 业内主流，逻辑直接 | — |
| 价格类型 | long（分为单位） | 避免浮点精度问题，性能优先 | — |

| 配置中心与缓存 | Redis + Caffeine + Pub/Sub | Redis 存储配置，Caffeine 5min TTL 兜底，Pub/Sub 实时失效 | [004](docs/adr/004-redis-caffeine-pubsub.md) |

## 当前进度

- [x] 阶段一：协议模型定义（Record + Sealed Interface）
- [x] 阶段二：请求接入与协议解析（ScopedValue）
- [x] 阶段三：DSP 并发扇出（Virtual Threads + invokeAll）
- [x] 阶段四：竞价决策（Pattern Matching + 一价拍卖）
- [x] 阶段五：响应组装 + 追踪埋点
- [x] 阶段六：缓存与配置热更新（Redis + Caffeine + Pub/Sub）

## 待办（跨阶段）

- [ ] DSP Win Notice 宏替换：对 DSP nurl 中的 `${AUCTION_PRICE}` 等 OpenRTB 标准宏做替换后回调通知 DSP 结算价（详见 `.scratch/response-assembly/issues/03-dsp-win-notice-macro.md`）

## 包结构

```
top.wain.bolt
├── client/         DSP 客户端抽象与路由
├── context/        ScopedValue 请求上下文
├── controller/     HTTP 入口
├── model/
│   ├── domain/     DspBidResult, AuctionResult, AdSource 等
│   ├── enums/      AdFormat, Carrier, DeviceType 等
│   ├── request/    BidRequest, Imp, Device, App
│   └── response/   BidResponse, Bid, SeatBid
├── repository/     AdSource/DspPlatform Redis+Caffeine Repository
├── cache/          Pub/Sub 缓存失效监听 + 预热
├── config/         CacheConfig, RedisConfig, DataSeeder
└── service/        BidService, AuctionService, DspFanOutService
```
