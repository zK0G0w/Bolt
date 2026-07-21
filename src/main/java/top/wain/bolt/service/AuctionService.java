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
                .filter(c -> passFloorCheck(c.bid(), c.source(), impBidFloor))
                .toList();

        Optional<Candidate> winner = candidates.stream()
                .max(Comparator.comparingLong(c -> c.bid().price()));

        return winner
                .map(c -> (AuctionResult) new AuctionResult.Win(
                        c.bid().adSourceId(),
                        c.bid().price(),
                        computeSettlePrice(c.bid().price(), c.source()),
                        c.bid().adPayload()))
                .orElse(new AuctionResult.NoBid());
    }

    /** 底价检查：出价必须同时高于媒体底价和引擎侧广告源底价 */
    private boolean passFloorCheck(DspBidResult.Success bid, AdSource source, long impBidFloor) {
        long sourceBidFloor = switch (source) {
            case AdSource.RtbSource rtb -> rtb.bidFloor();
            case AdSource.FixedPriceSource fixed -> fixed.fixedBidPrice();
        };
        return bid.price() >= impBidFloor && bid.price() >= sourceBidFloor;
    }

    /** 利润扣减：媒体结算价 = 出价 × (100 - profitRatio) / 100 */
    private long computeSettlePrice(long bidPrice, AdSource source) {
        int profitRatio = switch (source) {
            case AdSource.RtbSource rtb -> rtb.profitRatio();
            case AdSource.FixedPriceSource _ -> 0;
        };
        return bidPrice * (100 - profitRatio) / 100;
    }
}