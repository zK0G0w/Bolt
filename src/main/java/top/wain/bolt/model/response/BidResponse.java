package top.wain.bolt.model.response;

import java.util.List;

/**
 * @Description: 竞价响应，包含请求ID和所有广告位的出价结果
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 对应请求的唯一标识
 * @param bids 出价列表，每个 Bid 对应一个广告位的竞价结果
 */
public record BidResponse(
        String id,
        List<Bid> bids
) {}
