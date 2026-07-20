# Bolt

  基于 JDK 25 构建的轻量级 RTB（Real-Time Bidding）广告竞价引擎，聚焦核心竞价链路实现。

  ## 技术栈

  - JDK 25（Virtual Threads / ScopedValue / Sealed Types / Record）
  - Spring Boot 3.5 + WebMVC（开启 Virtual Threads）
  - OkHttp 4.x（DSP 请求 HTTP 客户端）
  - Caffeine 3.x（本地缓存）
  - Spring Data Redis + Lettuce（Redis 客户端）

  ## 核心链路

  请求接入 → 协议解析 → DSP 并发竞价（Virtual Threads + invokeAll 超时控制）→ 竞价决策（一价/二价）→ 响应组装 → 追踪埋点

  ## 项目目标

  以真实 RTB 场景为载体，实践 JDK 25 的现代语言特性与并发模型，对比传统线程池方案在代码复杂度和资源消耗上的差异。