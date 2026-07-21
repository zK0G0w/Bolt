package top.wain.bolt.context;

import top.wain.bolt.model.context.BidContext;

/**
 * @Description: 基于 ScopedValue 的请求上下文持有器，替代 ThreadLocal，Virtual Thread 友好
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
public final class BidScopedContext {

    public static final ScopedValue<BidContext> CURRENT = ScopedValue.newInstance();

    private BidScopedContext() {
    }

    public static BidContext current() {
        return CURRENT.get();
    }
}
