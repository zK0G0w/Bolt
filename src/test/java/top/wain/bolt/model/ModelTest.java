package top.wain.bolt.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.wain.bolt.model.enums.AdFormat;
import top.wain.bolt.model.enums.BidResult;
import top.wain.bolt.model.enums.Carrier;
import top.wain.bolt.model.enums.ConnectionType;
import top.wain.bolt.model.enums.DealType;
import top.wain.bolt.model.enums.DeviceType;
import top.wain.bolt.model.enums.OsType;
import top.wain.bolt.model.request.App;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.request.Device;
import top.wain.bolt.model.request.DeviceGeo;
import top.wain.bolt.model.request.DeviceHardware;
import top.wain.bolt.model.request.DeviceId;
import top.wain.bolt.model.request.Imp;
import top.wain.bolt.model.request.User;
import top.wain.bolt.model.response.Asset;
import top.wain.bolt.model.response.Bid;
import top.wain.bolt.model.response.BidResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @Description: 协议模型单元测试，验证 Record 序列化、Sealed Interface Pattern Matching、equals 语义
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
class ModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void bidRequest_jackson_roundTrip() throws Exception {
        BidRequest request = buildSampleRequest();

        String json = mapper.writeValueAsString(request);
        BidRequest deserialized = mapper.readValue(json, BidRequest.class);

        assertEquals(request.id(), deserialized.id());
        assertEquals(request.tmax(), deserialized.tmax());
        assertEquals(request.app(), deserialized.app());
        assertEquals(request.user(), deserialized.user());
        assertEquals(request.imp(), deserialized.imp());
    }

    @Test
    void bidResponse_jackson_roundTrip() throws Exception {
        Bid bid = new Bid(
                "imp-1", "dsp-001", 350L,
                "https://dsp.example.com/win",
                "https://dsp.example.com/loss",
                new Asset.Image(List.of("https://cdn.example.com/ad.jpg"), "image/jpeg", 1080, 1920, "测试广告", "广告描述"),
                "https://landing.example.com",
                List.of("https://track.example.com/imp"),
                List.of("https://track.example.com/clk")
        );
        BidResponse response = new BidResponse("req-123", List.of(bid));

        String json = mapper.writeValueAsString(response);
        BidResponse deserialized = mapper.readValue(json, BidResponse.class);

        assertEquals(response.id(), deserialized.id());
        assertEquals(response.bids().size(), deserialized.bids().size());
        assertEquals(bid.price(), deserialized.bids().getFirst().price());
    }

    @Test
    void adFormat_patternMatching_exhaustive() {
        List<AdFormat> formats = List.of(
                new AdFormat.Splash(1080, 1920),
                new AdFormat.Banner(640, 100),
                new AdFormat.NativeFeed(1, 30, 50),
                new AdFormat.Interstitial(600, 800),
                new AdFormat.RewardVideo(30)
        );

        for (AdFormat format : formats) {
            // switch 穷举所有子类型，编译器保证不遗漏
            String name = switch (format) {
                case AdFormat.Splash s -> "开屏 " + s.width() + "x" + s.height();
                case AdFormat.Banner b -> "横幅 " + b.width() + "x" + b.height();
                case AdFormat.NativeFeed n -> "信息流 图片数:" + n.imageCount();
                case AdFormat.Interstitial i -> "插屏 " + i.width() + "x" + i.height();
                case AdFormat.RewardVideo r -> "激励视频 时长:" + r.duration() + "s";
            };
            assert !name.isEmpty();
        }
    }

    @Test
    void dealType_patternMatching_exhaustive() {
        List<DealType> deals = List.of(
                new DealType.RTB(100L)
        );

        for (DealType deal : deals) {
            long floor = switch (deal) {
                case DealType.RTB rtb -> rtb.bidFloor();
            };
            assert floor > 0;
        }
    }

    @Test
    void bidResult_patternMatching_exhaustive() {
        Bid sampleBid = new Bid("imp-1", "dsp-001", 300L, null, null, null, null, List.of(), List.of());

        List<BidResult> results = List.of(
                new BidResult.Win(sampleBid, 250L),
                new BidResult.NoBid("无合适广告"),
                new BidResult.Timeout("dsp-002", 150L),
                new BidResult.Refused("dsp-003", "预算不足")
        );

        for (BidResult result : results) {
            String summary = switch (result) {
                case BidResult.Win w -> "胜出 结算价:" + w.settlePrice();
                case BidResult.NoBid nb -> "无出价 原因:" + nb.reason();
                case BidResult.Timeout t -> "超时 DSP:" + t.dspId() + " 耗时:" + t.elapsed() + "ms";
                case BidResult.Refused r -> "拒绝 DSP:" + r.dspId() + " 原因:" + r.reason();
            };
            assert !summary.isEmpty();
        }
    }

    @Test
    void asset_patternMatching_exhaustive() {
        List<Asset> assets = List.of(
                new Asset.Image(List.of("https://cdn.example.com/1.jpg"), "image/jpeg", 1080, 1920, "标题", "描述"),
                new Asset.Video("https://cdn.example.com/1.mp4", "video/mp4", 1080, 1920, "视频标题", 15000L, "https://cdn.example.com/cover.jpg")
        );

        for (Asset asset : assets) {
            String type = switch (asset) {
                case Asset.Image img -> "图片 数量:" + img.urls().size();
                case Asset.Video vid -> "视频 时长:" + vid.duration() + "ms";
            };
            assert !type.isEmpty();
        }
    }

    @Test
    void record_equals_and_hashCode() {
        App app1 = new App("app-001", "测试应用", "com.test.app", "1.0.0");
        App app2 = new App("app-001", "测试应用", "com.test.app", "1.0.0");
        assertEquals(app1, app2);
        assertEquals(app1.hashCode(), app2.hashCode());

        DeviceId id1 = new DeviceId("oaid-123", null, null, "android-456");
        DeviceId id2 = new DeviceId("oaid-123", null, null, "android-456");
        assertEquals(id1, id2);
    }

    @Test
    void enum_fromCode() {
        assertEquals(DeviceType.PHONE, DeviceType.fromCode(2));
        assertEquals(DeviceType.UNKNOWN, DeviceType.fromCode(99));
        assertEquals(ConnectionType.WIFI, ConnectionType.fromCode(1));
        assertEquals(OsType.ANDROID, OsType.fromCode(1));
        assertEquals(Carrier.CHINA_MOBILE, Carrier.fromCode(1));
    }

    @Test
    void asset_image_jackson_roundTrip() throws Exception {
        Asset.Image image = new Asset.Image(
                List.of("https://cdn.example.com/ad.jpg"),
                "image/jpeg", 1080, 1920, "标题", "描述"
        );

        String json = mapper.writeValueAsString(image);
        Asset.Image deserialized = mapper.readValue(json, Asset.Image.class);

        assertEquals(image, deserialized);
    }

    @Test
    void asset_video_jackson_roundTrip() throws Exception {
        Asset.Video video = new Asset.Video(
                "https://cdn.example.com/ad.mp4",
                "video/mp4", 1080, 1920, "视频", 15000L, "https://cdn.example.com/cover.jpg"
        );

        String json = mapper.writeValueAsString(video);
        Asset.Video deserialized = mapper.readValue(json, Asset.Video.class);

        assertEquals(video, deserialized);
    }

    @Test
    void device_nested_record_jackson_roundTrip() throws Exception {
        Device device = new Device(
                new DeviceId("oaid-123", "imei-456", null, "android-789"),
                new DeviceHardware(DeviceType.PHONE, "Xiaomi", "Mi 14", OsType.ANDROID, "15.0", 1080, 2400, ConnectionType.WIFI, Carrier.CHINA_MOBILE),
                new DeviceGeo(116.4f, 39.9f, "192.168.1.1", "CN", "北京")
        );

        String json = mapper.writeValueAsString(device);
        Device deserialized = mapper.readValue(json, Device.class);

        assertEquals(device.id(), deserialized.id());
        assertEquals(device.hardware().type(), deserialized.hardware().type());
        assertEquals(device.geo().region(), deserialized.geo().region());
    }

    private BidRequest buildSampleRequest() {
        DeviceId deviceId = new DeviceId("oaid-abc", "imei-def", null, "android-ghi");
        DeviceHardware hw = new DeviceHardware(DeviceType.PHONE, "OPPO", "Find X7", OsType.ANDROID, "14.0", 1080, 2412, ConnectionType.CELLULAR_5G, Carrier.CHINA_UNICOM);
        DeviceGeo geo = new DeviceGeo(121.47f, 31.23f, "10.0.0.1", "CN", "上海");
        Device device = new Device(deviceId, hw, geo);

        App app = new App("app-001", "测试APP", "com.test.app", "2.1.0");
        User user = new User("user-001", List.of("游戏", "科技"));

        Imp imp = new Imp("imp-001", new AdFormat.NativeFeed(1, 30, 50), new DealType.RTB(200L), 200L, 640, 320);

        return new BidRequest("req-001", imp, app, device, user, 100);
    }
}
