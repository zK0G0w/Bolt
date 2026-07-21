package top.wain.bolt.client;

import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.BidRequest;

/**
 * @Description: DSP 客户端统一接口，按 platformCode 路由到不同适配器实现
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
public interface DspClient {

    /**
     * 向 DSP 发送竞价请求
     * @param source 广告源配置（含超时、平台关联）
     * @param request 上游 BidRequest（透传）
     * @param dspBidFloor 经 PriceMarkup 加价后的底价，分/CPM
     * @return 出价结果（Success/NoBid/Timeout/Error）
     */
    DspBidResult sendBid(AdSource source, BidRequest request, long dspBidFloor) throws Exception;
}
