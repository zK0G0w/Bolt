package top.wain.bolt.service;

import org.junit.jupiter.api.Test;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.AuctionResult;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.repository.AdSourceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    private AdSourceRepository repoWith(List<AdSource> sources) {
        return new AdSourceRepository() {
            @Override
            public List<AdSource> findByAdPositionId(String adPositionId) {
                return sources;
            }

            @Override
            public Optional<AdSource> findById(String sourceId) {
                return sources.stream()
                        .filter(s -> s.sourceId().equals(sourceId))
                        .findFirst();
            }
        };
    }

    @Test
    void auction_highestBidWins() {
        List<AdSource> sources = List.of(
                rtbSource("src-001", 100L, 10),
                rtbSource("src-002", 100L, 10)
        );
        AuctionService service = new AuctionService(repoWith(sources));

        List<DspBidResult> results = List.of(
                new DspBidResult.Success("src-001", 200L, "ad1", "raw"),
                new DspBidResult.Success("src-002", 300L, "ad2", "raw")
        );

        AuctionResult result = service.auction(results, 100L);
        assertInstanceOf(AuctionResult.Win.class, result);

        AuctionResult.Win win = (AuctionResult.Win) result;
        assertEquals("src-002", win.adSourceId());
        assertEquals(300L, win.bidPrice());
    }

    @Test
    void auction_profitDeduction_appliedCorrectly() {
        List<AdSource> sources = List.of(rtbSource("src-001", 100L, 20));
        AuctionService service = new AuctionService(repoWith(sources));

        List<DspBidResult> results = List.of(
                new DspBidResult.Success("src-001", 500L, "ad", "raw")
        );

        AuctionResult.Win win = (AuctionResult.Win) service.auction(results, 100L);
        // 500 × (100 - 20) / 100 = 400
        assertEquals(500L, win.bidPrice());
        assertEquals(400L, win.settlePrice());
    }

    @Test
    void auction_belowImpFloor_filteredOut() {
        List<AdSource> sources = List.of(rtbSource("src-001", 50L, 10));
        AuctionService service = new AuctionService(repoWith(sources));

        List<DspBidResult> results = List.of(
                new DspBidResult.Success("src-001", 80L, "ad", "raw")
        );

        // 媒体底价 100，出价 80 < 100，被过滤
        AuctionResult result = service.auction(results, 100L);
        assertInstanceOf(AuctionResult.NoBid.class, result);
    }

    @Test
    void auction_belowSourceFloor_filteredOut() {
        List<AdSource> sources = List.of(rtbSource("src-001", 200L, 10));
        AuctionService service = new AuctionService(repoWith(sources));

        List<DspBidResult> results = List.of(
                new DspBidResult.Success("src-001", 150L, "ad", "raw")
        );

        // 广告源底价 200，出价 150 < 200，被过滤
        AuctionResult result = service.auction(results, 50L);
        assertInstanceOf(AuctionResult.NoBid.class, result);
    }

    @Test
    void auction_noSuccess_returnsNoBid() {
        List<AdSource> sources = List.of(rtbSource("src-001", 100L, 10));
        AuctionService service = new AuctionService(repoWith(sources));

        List<DspBidResult> results = List.of(
                new DspBidResult.NoBid("src-001"),
                new DspBidResult.Timeout("src-002")
        );

        AuctionResult result = service.auction(results, 100L);
        assertInstanceOf(AuctionResult.NoBid.class, result);
    }

    @Test
    void auction_fixedPriceSource_zeroProfitDeduction() {
        AdSource fixed = new AdSource.FixedPriceSource(
                "src-fix", "imp-001", "plat-001", "slot-001", 200, 300L
        );
        AuctionService service = new AuctionService(repoWith(List.of(fixed)));

        List<DspBidResult> results = List.of(
                new DspBidResult.Success("src-fix", 300L, "ad", "raw")
        );

        AuctionResult.Win win = (AuctionResult.Win) service.auction(results, 100L);
        // FixedPriceSource profitRatio=0，结算价=出价
        assertEquals(300L, win.settlePrice());
    }

    private AdSource.RtbSource rtbSource(String id, long bidFloor, int profitRatio) {
        return new AdSource.RtbSource(
                id, "imp-001", "plat-001", "slot-001",
                200, bidFloor, profitRatio, new AdSource.PriceMarkup.Ratio(0)
        );
    }
}