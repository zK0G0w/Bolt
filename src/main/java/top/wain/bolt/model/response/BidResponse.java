package top.wain.bolt.model.response;

import java.util.List;

/**
 * @Description: 竞价响应，包含请求ID和所有广告位的出价结果
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 对应请求的唯一标识（回传 BidRequest.id）
 * @param bidid 引擎侧竞价ID，用于追踪和对账
 * @param cur 货币代码（ISO-4217）
 * @param bids 出价列表，每个 Bid 对应一个广告位的竞价结果
 */
public record BidResponse(
        String id,
        String bidid,
        String cur,
        List<Bid> bids
) {}
