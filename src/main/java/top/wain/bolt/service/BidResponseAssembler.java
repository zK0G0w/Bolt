package top.wain.bolt.service;

import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AuctionResult;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.response.Bid;
import top.wain.bolt.model.response.BidResponse;
import top.wain.bolt.tracking.TrackingUrlGenerator;

import java.util.List;

/**
 * @Description: 竞价响应组装器，将竞价结果转换为 OpenRTB 响应结构，内化 tracking URL 生成
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class BidResponseAssembler {

    private static final String CURRENCY = "CNY";

    private final TrackingUrlGenerator trackingUrlGenerator;

    public BidResponseAssembler(TrackingUrlGenerator trackingUrlGenerator) {
        this.trackingUrlGenerator = trackingUrlGenerator;
    }

    public BidResponse assemble(AuctionResult auctionResult, BidRequest request, String bidId) {
        List<Bid> bids = switch (auctionResult) {
            case AuctionResult.Win win -> {
                String impUrl = trackingUrlGenerator.impressionUrl(bidId, win.adSourceId(), win.settlePrice());
                String clkUrl = trackingUrlGenerator.clickUrl(bidId, win.adSourceId(), win.settlePrice(), "");
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

        return new BidResponse(request.id(), bidId, CURRENCY, bids);
    }
}
