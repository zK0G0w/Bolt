# ADR-002: ScopedValue 替代 ThreadLocal 传递请求上下文

**状态**: 已采纳  
**日期**: 2025-06  

## 背景

RTB 链路中多个 Service 需要访问请求级上下文（requestId、请求时间、来源 IP）。传统方案是 ThreadLocal，但项目使用 Virtual Threads，ThreadLocal 在 VT 场景下存在已知问题。

## 决策

使用 `ScopedValue<BidContext>`，Controller 层通过 `ScopedValue.runWhere` 绑定：

```java
public final class BidScopedContext {
    private static final ScopedValue<BidContext> CONTEXT = ScopedValue.newInstance();
    
    public static BidContext current() { return CONTEXT.get(); }
    public static <T> T run(BidContext ctx, Callable<T> op) { ... }
}
```

## ThreadLocal 的问题（Virtual Thread 场景）

1. **内存**: VT 可能有百万级实例，每个 VT 的 ThreadLocalMap 占用累积可观
2. **清理**: 忘记 remove() 导致泄漏，VT 被复用时更隐蔽
3. **语义**: ThreadLocal 的可变性与请求上下文的不可变语义矛盾

## ScopedValue 的优势

- 不可变：绑定后无法修改，天然线程安全
- 生命周期明确：`runWhere` 作用域结束自动解绑，无泄漏风险
- 轻量：无 ThreadLocalMap 开销，对 VT 友好
- 继承性：子 VT 自动继承父线程的 ScopedValue 绑定

## 约束

- 调用 `current()` 必须在 `runWhere` 作用域内，否则抛 NoSuchElementException
- JDK 25 中 ScopedValue 为正式 API（非 preview）
