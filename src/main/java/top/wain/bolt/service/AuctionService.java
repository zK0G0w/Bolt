package top.wain.bolt.service;

import org.springframework.stereotype.Service;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.AuctionResult;
import top.wain.bolt.model.domain.DspBidResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Description: 竞价决策服务，一价拍卖：底价过滤 → 出价排序 → 选赢家 → 利润扣减
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Service
public class AuctionService {

    /**
     * 执行一价拍卖
     * @param results DSP 扇出返回的出价结果列表
     * @param impBidFloor 媒体侧广告位底价（分/CPM）
     * @param sources 已解析的广告源配置映射（sourceId → AdSource）
     * @return 竞价结果：Win 或 NoBid
     */
    public AuctionResult auction(List<DspBidResult> results, long impBidFloor, Map<String, AdSource> sources) {
        record Candidate(DspBidResult.Success bid, AdSource source) {}

        List<Candidate> candidates = results.stream()
                .filter(r -> r instanceof DspBidResult.Success)
                .map(r -> (DspBidResult.Success) r)
                .map(s -> new Candidate(s, sources.get(s.adSourceId())))
                .filter(c -> c.source() != null)
                // 底价检查：出价必须同时高于媒体底价和引擎侧广告源底价
                .filter(c -> c.bid().price() >= impBidFloor && c.bid().price() >= c.source().floorPrice())
                .toList();

        Optional<Candidate> winner = candidates.stream()
                .max(Comparator.comparingLong(c -> c.bid().price()));

        return winner
                .map(c -> (AuctionResult) new AuctionResult.Win(
                        c.bid().adSourceId(),
                        c.bid().price(),
                        c.source().settlePrice(c.bid().price()),
                        c.bid().adPayload()))
                .orElse(new AuctionResult.NoBid());
    }
}