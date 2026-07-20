package top.wain.bolt.model.context;

/**
 * @Description: 竞价请求上下文，携带贯穿链路的元信息
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param requestId 请求唯一标识
 * @param requestTime 请求到达时间戳，单位毫秒
 * @param sourceIp 请求来源IP
 */
public record BidContext(
        String requestId,
        long requestTime,
        String sourceIp
) {}
