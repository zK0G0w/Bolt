# ADR-004: Redis + Caffeine + Pub/Sub 配置热更新架构

## 状态

已采纳 (2026-07-21)

## 背景

阶段一至五的 Repository 层使用硬编码 InMemory Map，无法动态更新配置。阶段六需要支持外部管理系统修改 AdSource / DspPlatform 配置后，引擎实时感知变更。

## 决策

采用 **Redis 持久存储 + Caffeine 本地缓存 + Redis Pub/Sub 主动失效** 的三层架构。

### 数据流

```
管理端 SET Redis → PUBLISH bolt:cache:invalidate
    → CacheInvalidationListener evict Caffeine
    → 下次请求 cache miss → read-through 回源 Redis
```

### 兜底机制

Caffeine 设置 5 分钟 `expireAfterWrite`，即使 Pub/Sub 消息丢失（网络闪断），最多 5 分钟后自动回源。

## 否决方案

| 方案 | 否决理由 |
|------|---------|
| Nacos / Apollo | 单节点学习项目，引入独立配置中心纯运维成本 |
| Kafka 推送（参考项目方案） | 单实例不需要广播语义，Kafka 过重 |
| 纯轮询（@Scheduled） | 延迟高，无法实时生效 |
| Caffeine 无 TTL + 纯 Pub/Sub | 消息丢失后永远不一致，无兜底 |

## 后果

- Repository 接口不变，服务层零改动
- 测试需要 mock Redis（已通过 MockRedisTestConfig 解决）
- 外部管理端需同时执行 SET + PUBLISH 两步操作
