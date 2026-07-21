package top.wain.bolt.service;

import org.junit.jupiter.api.Test;
import top.wain.bolt.client.DspClient;
import top.wain.bolt.client.DspClientRouter;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.domain.DspPlatform;
import top.wain.bolt.model.enums.AdFormat;
import top.wain.bolt.model.enums.DealType;
import top.wain.bolt.model.request.*;
import top.wain.bolt.model.enums.*;
import top.wain.bolt.repository.AdSourceRepository;
import top.wain.bolt.repository.DspPlatformRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DspFanOutServiceTest {

    private BidRequest sampleRequest(String impId, int tmax) {
        return new BidRequest(
                "req-001",
                List.of(new Imp(impId, new AdFormat.NativeFeed(1, 30, 50), new DealType.RTB(200L), 200L, 640, 320)),
                new App("app-001", "TestApp", "com.test", "1.0"),
                new Device(
                        new DeviceId("oaid-123", null, null, null),
                        new DeviceHardware(DeviceType.PHONE, "Xiaomi", "14", OsType.ANDROID, "15", 1080, 2400, ConnectionType.WIFI, Carrier.CHINA_MOBILE),
                        new DeviceGeo(116.4f, 39.9f, "10.0.0.1", "CN", "北京")
                ),
                new User("user-001", List.of("游戏")),
                tmax
        );
    }

    private DspPlatform platform(String id, String code) {
        return new DspPlatform(id, "Test-" + code, code, "https://dsp.test.com/bid", 1000, 50);
    }

    @Test
    void fanOut_normalCase_collectsAllSuccess() {
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(20)),
                new AdSource.RtbSource("src-002", "imp-001", "plat-002", "slot-002", 200, 150L, 15, new AdSource.PriceMarkup.Fixed(200L))
        );

        DspClient fastClient = (source, request, floor) ->
                new DspBidResult.Success(source.sourceId(), floor + 50, "ad", "raw");

        DspFanOutService service = buildService(sources, fastClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 500));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r instanceof DspBidResult.Success));

        // Verify PriceMarkup.Ratio: 100 * (100+20)/100 = 120, then +50 = 170
        DspBidResult.Success s1 = (DspBidResult.Success) results.get(0);
        assertEquals(170L, s1.price());

        // Verify PriceMarkup.Fixed: floor=200, then +50 = 250
        DspBidResult.Success s2 = (DspBidResult.Success) results.get(1);
        assertEquals(250L, s2.price());
    }

    @Test
    void fanOut_timeout_returnTimeoutResult() {
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(10))
        );

        // Client sleeps longer than total deadline
        DspClient slowClient = (source, request, floor) -> {
            Thread.sleep(2000);
            return new DspBidResult.Success(source.sourceId(), 999L, "ad", "raw");
        };

        DspFanOutService service = buildService(sources, slowClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 50));

        assertEquals(1, results.size());
        assertInstanceOf(DspBidResult.Timeout.class, results.getFirst());
    }

    @Test
    void fanOut_partialFailure_collectsSuccessAndTimeout() {
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(10)),
                new AdSource.RtbSource("src-002", "imp-001", "plat-002", "slot-002", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(10))
        );

        DspClient mixedClient = (source, request, floor) -> {
            if ("src-001".equals(source.sourceId())) {
                return new DspBidResult.Success(source.sourceId(), 200L, "ad", "raw");
            }
            // src-002 sleeps forever
            Thread.sleep(5000);
            return new DspBidResult.Success(source.sourceId(), 100L, "ad", "raw");
        };

        DspFanOutService service = buildService(sources, mixedClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 100));

        assertEquals(2, results.size());
        long successCount = results.stream().filter(r -> r instanceof DspBidResult.Success).count();
        long timeoutCount = results.stream().filter(r -> r instanceof DspBidResult.Timeout).count();
        assertEquals(1, successCount);
        assertEquals(1, timeoutCount);
    }

    @Test
    void fanOut_allFailure_returnsEmptySuccessList() {
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(10))
        );

        DspClient errorClient = (source, request, floor) -> {
            throw new RuntimeException("connection refused");
        };

        DspFanOutService service = buildService(sources, errorClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 200));

        assertEquals(1, results.size());
        assertInstanceOf(DspBidResult.Error.class, results.getFirst());
    }

    @Test
    void fanOut_noMatchingAdSources_returnsEmpty() {
        DspClient client = (source, request, floor) ->
                new DspBidResult.Success(source.sourceId(), 100L, "ad", "raw");

        DspFanOutService service = buildService(List.of(), client);
        List<DspBidResult> results = service.fanOut(sampleRequest("unknown-imp", 200));

        assertTrue(results.isEmpty());
    }

    @Test
    void fanOut_priceMarkupRatio_appliedCorrectly() {
        // bidFloor=200, Ratio(30) → 200 * 130/100 = 260
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 200L, 10, new AdSource.PriceMarkup.Ratio(30))
        );

        DspClient echoClient = (source, request, floor) ->
                new DspBidResult.Success(source.sourceId(), floor, "ad", "raw");

        DspFanOutService service = buildService(sources, echoClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 500));

        DspBidResult.Success s = (DspBidResult.Success) results.getFirst();
        assertEquals(260L, s.price());
    }

    @Test
    void fanOut_priceMarkupFixed_appliedCorrectly() {
        // Fixed(350) → dspBidFloor = 350 regardless of original bidFloor
        List<AdSource> sources = List.of(
                new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 200L, 10, new AdSource.PriceMarkup.Fixed(350L))
        );

        DspClient echoClient = (source, request, floor) ->
                new DspBidResult.Success(source.sourceId(), floor, "ad", "raw");

        DspFanOutService service = buildService(sources, echoClient);
        List<DspBidResult> results = service.fanOut(sampleRequest("imp-001", 500));

        DspBidResult.Success s = (DspBidResult.Success) results.getFirst();
        assertEquals(350L, s.price());
    }

    private DspFanOutService buildService(List<AdSource> sources, DspClient client) {
        AdSourceRepository adSourceRepo = adPositionId ->
                "imp-001".equals(adPositionId) ? sources : List.of();

        Map<String, DspPlatform> platforms = Map.of(
                "plat-001", platform("plat-001", "huawei"),
                "plat-002", platform("plat-002", "gdt")
        );
        DspPlatformRepository platformRepo = id -> Optional.ofNullable(platforms.get(id));

        DspClientRouter router = new DspClientRouter(List.of(client)) {
            @Override
            public DspClient route(String platformCode) {
                return client;
            }
        };

        return new DspFanOutService(adSourceRepo, platformRepo, router);
    }
}
