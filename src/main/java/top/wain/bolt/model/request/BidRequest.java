package top.wain.bolt.model.request;

/**
 * @Description: 竞价请求，引擎内部统一模型，贯穿整个竞价链路。每次请求对应单个广告位。
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 请求唯一标识
 * @param imp 广告位，一次请求对应一个展示机会
 * @param app 应用信息
 * @param device 设备信息
 * @param user 用户信息
 * @param tmax 最大等待时间，单位毫秒，超时未返回视为无出价
 */
public record BidRequest(
        String id,
        Imp imp,
        App app,
        Device device,
        User user,
        int tmax
) {}
