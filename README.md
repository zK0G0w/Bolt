# Bolt

  基于 JDK 25 构建的轻量级 RTB（Real-Time Bidding）广告竞价引擎，聚焦核心竞价链路实现。

  ## 技术栈

  - JDK 25（Virtual Threads / Structured Concurrency / ScopedValue / Sealed Types）
  - Helidon SE 4.x（HTTP 服务）
  - java.net.http.HttpClient（DSP 请求）
  - Caffeine 3.x（本地缓存）
  - Lettuce（Redis 客户端）

  ## 核心链路

  请求接入 → 协议解析 → DSP 并发竞价（Structured Concurrency + 超时控制）→ 竞价决策（一价/二价）→ 响应组装 → 追踪埋点

  ## 项目目标

  以真实 RTB 场景为载体，实践 JDK 21+ 的现代并发编程模型，对比传统线程池方案在代码复杂度和资源消耗上的差异。