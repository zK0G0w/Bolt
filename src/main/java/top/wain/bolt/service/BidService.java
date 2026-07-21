package top.wain.bolt.service;

import org.springframework.stereotype.Service;
import top.wain.bolt.context.BidScopedContext;
import top.wain.bolt.model.context.BidContext;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.response.Bid;
import top.wain.bolt.model.response.BidResponse;

import java.util.List;

/**
 * @Description: 竞价服务，顶层编排者：调 DSP 扇出拿出价 → 竞价决策选赢家 → 组装响应
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Service
public class BidService {

    private final DspFanOutService dspFanOutService;

    public BidService(DspFanOutService dspFanOutService) {
        this.dspFanOutService = dspFanOutService;
    }

    public BidResponse bid(BidRequest request) {
        BidContext ctx = BidScopedContext.current();

        List<DspBidResult> results = dspFanOutService.fanOut(request);

        // TODO: 阶段四 — 替换为竞价决策逻辑（排序、比价、利润扣减）
        List<Bid> bids = results.stream()
                .filter(r -> r instanceof DspBidResult.Success)
                .map(r -> (DspBidResult.Success) r)
                .findFirst()
                .map(s -> List.of(new Bid(
                        request.imps().getFirst().id(),
                        s.adSourceId(),
                        s.price(),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()
                )))
                .orElse(List.of());

        return new BidResponse(request.id(), bids);
    }
}
