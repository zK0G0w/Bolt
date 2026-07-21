package top.wain.bolt.model.domain;

/**
 * @Description: 竞价决策结果，一价拍卖产出。胜出时携带赢家信息和媒体结算价；无赢家时为 NoBid。
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
public sealed interface AuctionResult {

    /**
     * 竞价胜出
     * @param adSourceId 胜出广告源ID
     * @param bidPrice DSP 原始出价（分/CPM），即广告主结算价
     * @param settlePrice 媒体结算价（分/CPM），= bidPrice × (100 - profitRatio) / 100
     * @param adPayload 素材信息
     */
    record Win(String adSourceId, long bidPrice, long settlePrice, String adPayload) implements AuctionResult {}

    /** 无有效出价（所有候选被底价过滤或无 Success） */
    record NoBid() implements AuctionResult {}
}
