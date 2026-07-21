package top.wain.bolt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.wain.bolt.context.BidScopedContext;
import top.wain.bolt.model.context.BidContext;
import top.wain.bolt.model.domain.AuctionResult;
import top.wain.bolt.model.domain.FanOutResult;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.response.BidResponse;

import java.util.UUID;

/**
 * @Description: 竞价服务，顶层编排者：扇出 → 竞价 → 组装
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Service
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    private final DspFanOutService dspFanOutService;
    private final AuctionService auctionService;
    private final BidResponseAssembler responseAssembler;

    public BidService(DspFanOutService dspFanOutService, AuctionService auctionService,
                      BidResponseAssembler responseAssembler) {
        this.dspFanOutService = dspFanOutService;
        this.auctionService = auctionService;
        this.responseAssembler = responseAssembler;
    }

    public BidResponse bid(BidRequest request) {
        BidContext ctx = BidScopedContext.current();

        FanOutResult fanOutResult = dspFanOutService.fanOut(request);

        AuctionResult auctionResult = auctionService.auction(
                fanOutResult.results(), request.imp().bidFloor(), fanOutResult.resolvedSources());

        switch (auctionResult) {
            case AuctionResult.Win win -> log.info("竞价胜出 reqId={} adSourceId={} bidPrice={} settlePrice={}",
                    ctx.requestId(), win.adSourceId(), win.bidPrice(), win.settlePrice());
            case AuctionResult.NoBid() -> log.info("竞价无赢家 reqId={} candidates={}",
                    ctx.requestId(), fanOutResult.results().size());
        }

        String bidId = UUID.randomUUID().toString();
        return responseAssembler.assemble(auctionResult, request, bidId);
    }
}
