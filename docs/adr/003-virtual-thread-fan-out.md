# ADR-003: Virtual Thread + invokeAll 并发扇出

**状态**: 已采纳  
**日期**: 2025-06  

## 背景

竞价请求需要并发请求多个 DSP（典型 3-10 个），总超时约 100-200ms。需要并发模型满足：统一超时控制、部分失败容忍、代码简洁。

## 决策

每次竞价请求新建 `newVirtualThreadPerTaskExecutor`，用 `invokeAll(tasks, timeout, unit)` 统一超时：

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    long deadline = tmax - SAFETY_MARGIN_MS;  // 预留 20ms 给响应组装
    List<Future<DspBidResult>> futures = executor.invokeAll(tasks, deadline, MILLISECONDS);
    return collectResults(futures);  // cancelled → Timeout, exception → Error
}
```

## 否决方案

| 方案 | 否决理由 |
|------|---------|
| CompletableFuture.allOf | 没有内建统一超时，需手动 completeOnTimeout 逐个管理 |
| 固定线程池 + VT | VT 极轻量（~1KB），池化增加管理复杂度无收益 |
| StructuredTaskScope | JDK 25 仍为 preview，且 invokeAll 已满足需求 |

## 关键设计点

1. **每请求新建 executor**: VT 不需要池化，try-with-resources 保证资源回收
2. **安全余量 20ms**: `tmax - 20` 确保扇出超时后仍有时间组装响应
3. **最低 10ms 兜底**: 防止 tmax 极小时 deadline 变为负数
4. **Future 状态映射**: `isCancelled()` → Timeout，`ExecutionException` → Error

## 约束

- `invokeAll` 阻塞调用线程直到全部完成/超时，这在 VT 上是安全的（不 pin carrier）
- 但 OkHttp 内部可能 pin carrier thread（synchronized 块），需关注
