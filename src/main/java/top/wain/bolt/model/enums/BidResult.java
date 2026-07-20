package top.wain.bolt.model.enums;

import top.wain.bolt.model.response.Bid;

/**
 * @Description: 竞价结果，用 sealed interface 建模所有可能的竞价终态，配合 Pattern Matching 使用
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public sealed interface BidResult {

    /**
     * 竞价胜出
     * @param bid 胜出的出价
     * @param settlePrice 结算价，单位：分/CPM（二价拍卖时低于出价）
     */
    record Win(Bid bid, long settlePrice) implements BidResult {}

    /**
     * DSP 无出价
     * @param reason 无出价原因
     */
    record NoBid(String reason) implements BidResult {}

    /**
     * DSP 响应超时
     * @param dspId 超时的DSP标识
     * @param elapsed 实际耗时，单位毫秒
     */
    record Timeout(String dspId, long elapsed) implements BidResult {}

    /**
     * DSP 主动拒绝出价
     * @param dspId 拒绝的DSP标识
     * @param reason 拒绝原因
     */
    record Refused(String dspId, String reason) implements BidResult {}
}
