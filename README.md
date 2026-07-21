# Bolt

![JDK](https://img.shields.io/badge/JDK-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![License](https://img.shields.io/badge/License-MIT-blue)

基于 JDK 25 构建的轻量级 RTB（Real-Time Bidding）广告竞价引擎，以真实竞价场景为载体，实践 Virtual Threads、ScopedValue、Sealed Types 等现代 Java 特性。

## 核心亮点

| 模块 | 实现 | JDK 25 特性 |
|------|------|-------------|
| 并发扇出 | 每请求 VT Executor + invokeAll 统一超时 | Virtual Threads |
| 请求上下文 | ScopedValue 替代 ThreadLocal | ScopedValue |
| DSP 响应建模 | Sealed Interface (Success/NoBid/Timeout/Error) | Sealed Types + Record |
| 竞价决策 | switch 表达式 + 类型模式匹配 | Pattern Matching |
| 配置中心 | Redis 存储 + Caffeine 本地缓存 + Pub/Sub 实时失效 | — |
| 追踪埋点 | AES 加密参数 + 时间戳防重放 | — |

## 架构

```mermaid
flowchart LR
    A[POST /bid] --> B[ScopedValue 绑定请求上下文]
    B --> C[DspFanOutService]
    C -->|Virtual Threads<br/>invokeAll| D1[DSP 1]
    C -->|Virtual Threads<br/>invokeAll| D2[DSP 2]
    C -->|Virtual Threads<br/>invokeAll| D3[DSP N]
    D1 --> E[AuctionService<br/>Pattern Matching 竞价决策]
    D2 --> E
    D3 --> E
    E --> F[BidResponse 组装<br/>+ 追踪 URL 生成]
```

## 快速启动

### Docker（推荐）

```bash
git clone https://github.com/zK0G0w/Bolt.git
cd Bolt
make up       # 启动 Redis + App
make bid      # 发起竞价请求
```

### 本地开发

前置依赖：JDK 25、Redis 7.x

```bash
make dev      # 启动应用（连接本机 Redis）
make bid      # 发起竞价请求
```

### 常用命令

```bash
make help     # 查看所有可用命令
make bench    # 压测（200 并发）
make test     # 运行测试
make down     # 停止容器
```

## 性能基准

测试环境：MacBook Pro M3 Max / 64GB RAM / JDK 25 / Redis 7.x

测试工具：[hey](https://github.com/rakyll/hey)，MockDspClient 模拟 30-120ms 随机延迟

| 并发连接 | QPS | p50 | p99 | 说明 |
|---------|-----|-----|-----|------|
| 200 | 2,127 | 97ms | 123ms | 轻负载 |
| 1000 | **10,260** | 100ms | 140ms | Virtual Threads 线性扩展 |

> 响应时间几乎不随并发数增长（稳定 ~100ms），QPS 与并发数呈线性关系——没有线程池上限，并发从 200 扩到 1000，延迟不退化，吞吐翻 5 倍。

## 项目目标

以真实 RTB 场景为载体，实践 JDK 25 的现代语言特性与并发模型，对比传统线程池方案在代码复杂度和资源消耗上的差异。

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/bid` | 竞价请求，返回胜出 DSP 的出价响应 |
| `GET` | `/i` | 展示追踪（impression） |
| `GET` | `/c` | 点击追踪（click） |

## 配置

主要可调参数（通过环境变量或 `application-dev.yml`）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | 9292 | 服务端口 |
| `REDIS_PASSWORD` | redis123456 | Redis 密码 |
| `SPRING_PROFILES_ACTIVE` | dev | 运行环境 |
| `TOMCAT_MAX_CONNECTIONS` | 10000 | 最大连接数 |
| `TOMCAT_ACCEPT_COUNT` | 1000 | 等待队列长度 |

## License

[MIT](LICENSE)
