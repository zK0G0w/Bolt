package top.wain.bolt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.wain.bolt.context.BidScopedContext;
import top.wain.bolt.model.context.BidContext;
import top.wain.bolt.model.domain.AuctionResult;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.response.Bid;
import top.wain.bolt.model.response.BidResponse;
import top.wain.bolt.tracking.TrackingUrlGenerator;

import java.util.List;
import java.util.UUID;

/**
 * @Description: 竞价服务，顶层编排者：调 DSP 扇出拿出价 → 竞价决策选赢家 → 组装响应
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Service
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);
    private static final String CURRENCY = "CNY";

    private final DspFanOutService dspFanOutService;
    private final AuctionService auctionService;
    private final TrackingUrlGenerator trackingUrlGenerator;

    public BidService(DspFanOutService dspFanOutService, AuctionService auctionService,
                      TrackingUrlGenerator trackingUrlGenerator) {
        this.dspFanOutService = dspFanOutService;
        this.auctionService = auctionService;
        this.trackingUrlGenerator = trackingUrlGenerator;
    }

    public BidResponse bid(BidRequest request) {
        BidContext ctx = BidScopedContext.current();

        List<DspBidResult> results = dspFanOutService.fanOut(request);

        AuctionResult auctionResult = auctionService.auction(results, request.imp().bidFloor());

        switch (auctionResult) {
            case AuctionResult.Win win -> log.info("竞价胜出 reqId={} adSourceId={} bidPrice={} settlePrice={}",
                    ctx.requestId(), win.adSourceId(), win.bidPrice(), win.settlePrice());
            case AuctionResult.NoBid() -> log.info("竞价无赢家 reqId={} candidates={}",
                    ctx.requestId(), results.size());
        }

        String bidid = UUID.randomUUID().toString();

        List<Bid> bids = switch (auctionResult) {
            case AuctionResult.Win win -> {
                String impUrl = trackingUrlGenerator.impressionUrl(bidid, win.adSourceId(), win.settlePrice());
                String clkUrl = trackingUrlGenerator.clickUrl(bidid, win.adSourceId(), win.settlePrice(), "");
                yield List.of(new Bid(
                        request.imp().id(),
                        win.adSourceId(),
                        win.settlePrice(),
                        null,
                        null,
                        null,
                        null,
                        List.of(impUrl),
                        List.of(clkUrl)
                ));
            }
            case AuctionResult.NoBid() -> List.of();
        };

        return new BidResponse(request.id(), bidid, CURRENCY, bids);
    }
}
