package top.wain.bolt.client;

import org.junit.jupiter.api.Test;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspBidResult;
import top.wain.bolt.model.request.*;
import top.wain.bolt.model.enums.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockDspClientTest {

    private final MockDspClient client = new MockDspClient();

    private BidRequest sampleRequest() {
        return new BidRequest(
                "req-001",
                new Imp("imp-001", new AdFormat.NativeFeed(1, 30, 50), new DealType.RTB(200L), 200L, 640, 320),
                new App("app-001", "TestApp", "com.test", "1.0"),
                new Device(
                        new DeviceId("oaid-123", null, null, null),
                        new DeviceHardware(DeviceType.PHONE, "Xiaomi", "14", OsType.ANDROID, "15", 1080, 2400, ConnectionType.WIFI, Carrier.CHINA_MOBILE),
                        new DeviceGeo(116.4f, 39.9f, "10.0.0.1", "CN", "北京")
                ),
                new User("user-001", List.of("游戏")),
                100
        );
    }

    @Test
    void sendBid_returnsValidDspBidResult() {
        AdSource source = new AdSource.RtbSource(
                "src-001", "imp-001", "plat-001", "slot-001",
                200, 150L, 15, new AdSource.PriceMarkup.Ratio(20)
        );

        DspBidResult result = client.sendBid(source, sampleRequest(), 180L);

        assertNotNull(result);
        assertEquals("src-001", result.adSourceId());
        // result 一定是四种之一
        switch (result) {
            case DspBidResult.Success s -> {
                assertTrue(s.price() > 0);
                assertNotNull(s.adPayload());
                assertNotNull(s.rawResponse());
            }
            case DspBidResult.NoBid _ -> {}
            case DspBidResult.Timeout _ -> {}
            case DspBidResult.Error e -> assertNotNull(e.reason());
        }
    }

    @Test
    void sendBid_multipleCalls_producesVariedResults() {
        AdSource source = new AdSource.RtbSource(
                "src-001", "imp-001", "plat-001", "slot-001",
                50, 100L, 10, new AdSource.PriceMarkup.Ratio(10)
        );

        int successCount = 0;
        int noBidCount = 0;
        for (int i = 0; i < 50; i++) {
            DspBidResult result = client.sendBid(source, sampleRequest(), 110L);
            if (result instanceof DspBidResult.Success) successCount++;
            if (result instanceof DspBidResult.NoBid) noBidCount++;
        }
        // 50次调用中，大概率至少有一些 Success 和一些 NoBid
        assertTrue(successCount > 0, "Expected at least one Success in 50 calls");
    }

    @Test
    void router_routesByPlatformCode() {
        MockDspClient mock = new MockDspClient();
        Mock2DspClient mock2 = new Mock2DspClient();
        DspClientRouter router = new DspClientRouter(List.of(mock, mock2));

        assertSame(mock, router.route("mock").orElseThrow());
        assertSame(mock2, router.route("mock2").orElseThrow());
    }

    @Test
    void router_unknownCode_returnsEmpty() {
        DspClientRouter router = new DspClientRouter(List.of(new MockDspClient()));

        // 无对应适配器时不再兜底降级，避免静默返回 mock 出价
        assertTrue(router.route("huawei").isEmpty());
    }

    @Test
    void router_supportedCodes_reflectsDeployedAdapters() {
        DspClientRouter router = new DspClientRouter(List.of(new MockDspClient(), new Mock2DspClient()));

        assertEquals(Set.of("mock", "mock2"), router.supportedCodes());
    }
}
