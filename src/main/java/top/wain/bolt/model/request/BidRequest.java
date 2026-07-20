package top.wain.bolt.model.request;

import java.util.List;

/**
 * @Description: 竞价请求，引擎内部统一模型，贯穿整个竞价链路
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 请求唯一标识
 * @param imps 广告位列表，一次请求可携带多个广告位
 * @param app 应用信息
 * @param device 设备信息
 * @param user 用户信息
 * @param tmax 最大等待时间，单位毫秒，超时未返回视为无出价
 */
public record BidRequest(
        String id,
        List<Imp> imps,
        App app,
        Device device,
        User user,
        int tmax
) {}
