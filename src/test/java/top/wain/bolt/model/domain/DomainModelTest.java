package top.wain.bolt.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Description: 域模型单元测试，验证 Sealed Interface Pattern Matching 分发、PriceMarkup 加价计算、Record 语义
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
class DomainModelTest {

    @Test
    void adSource_patternMatching_exhaustive() {
        AdSource rtb = new AdSource.RtbSource(
                "src-001", "pos-001", "plat-001", "slot-001",
                300, 200L, 15, new AdSource.PriceMarkup.Ratio(20)
        );
        AdSource fixed = new AdSource.FixedPriceSource(
                "src-002", "pos-001", "plat-002", "slot-002",
                300, 500L
        );

        // 验证 switch 穷举和公共访问器
        for (AdSource source : new AdSource[]{rtb, fixed}) {
            String desc = switch (source) {
                case AdSource.RtbSource r -> "RTB 底价:" + r.bidFloor();
                case AdSource.FixedPriceSource f -> "固定出价:" + f.fixedBidPrice();
            };
            assert !desc.isEmpty();
            // 公共访问器可统一调用
            assert source.sourceId() != null;
            assert source.platformId() != null;
            assert source.timeoutMs() == 300;
        }
    }

    @Test
    void priceMarkup_ratio_calculation() {
        // 比例加价：底价200，加价20% → 200 * (100 + 20) / 100 = 240
        AdSource.PriceMarkup markup = new AdSource.PriceMarkup.Ratio(20);
        long bidFloor = 200L;

        long dspFloor = switch (markup) {
            case AdSource.PriceMarkup.Ratio(var percent) -> bidFloor * (100 + percent) / 100;
            case AdSource.PriceMarkup.Fixed(var price) -> price;
        };

        assertEquals(240L, dspFloor);
    }

    @Test
    void priceMarkup_fixed_calculation() {
        // 固定提交价：无论底价多少，直接使用固定值350
        AdSource.PriceMarkup markup = new AdSource.PriceMarkup.Fixed(350L);
        long bidFloor = 200L;

        long dspFloor = switch (markup) {
            case AdSource.PriceMarkup.Ratio(var percent) -> bidFloor * (100 + percent) / 100;
            case AdSource.PriceMarkup.Fixed(var price) -> price;
        };

        assertEquals(350L, dspFloor);
    }

    @Test
    void adSource_record_equals() {
        AdSource.RtbSource a = new AdSource.RtbSource(
                "src-001", "pos-001", "plat-001", "slot-001",
                300, 200L, 15, new AdSource.PriceMarkup.Ratio(20)
        );
        AdSource.RtbSource b = new AdSource.RtbSource(
                "src-001", "pos-001", "plat-001", "slot-001",
                300, 200L, 15, new AdSource.PriceMarkup.Ratio(20)
        );
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void sellPlatform_record_basics() {
        SellPlatform platform = new SellPlatform(
                "plat-001", "华为ADX", "huawei",
                "https://adx.huawei.com/bid", 1000, 50
        );

        assertEquals("plat-001", platform.platformId());
        assertEquals("华为ADX", platform.name());
        assertEquals("huawei", platform.platformCode());
        assertEquals(1000, platform.trafficQps());
        assertEquals(50, platform.trafficFrequency());
    }

    @Test
    void sellPlatform_record_equals() {
        SellPlatform a = new SellPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        SellPlatform b = new SellPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void adSource_commonAccessor_polymorphism() {
        // 验证通过 sealed interface 公共方法统一访问不同子类型
        AdSource rtb = new AdSource.RtbSource("src-001", "pos-001", "plat-001", "slot-001", 200, 100L, 10, new AdSource.PriceMarkup.Ratio(10));
        AdSource fixed = new AdSource.FixedPriceSource("src-002", "pos-002", "plat-002", "slot-002", 400, 600L);

        assertEquals("plat-001", rtb.platformId());
        assertEquals("plat-002", fixed.platformId());
        assertEquals(200, rtb.timeoutMs());
        assertEquals(400, fixed.timeoutMs());
    }

    @Test
    void priceMarkup_nested_sealed_patternMatching() {
        // 验证嵌套 sealed interface 的解构模式匹配
        AdSource.RtbSource source = new AdSource.RtbSource(
                "src-001", "pos-001", "plat-001", "slot-001",
                300, 150L, 20, new AdSource.PriceMarkup.Ratio(30)
        );

        long dspFloor = switch (source.markup()) {
            case AdSource.PriceMarkup.Ratio(var percent) -> source.bidFloor() * (100 + percent) / 100;
            case AdSource.PriceMarkup.Fixed(var price) -> price;
        };

        // 150 * 130 / 100 = 195
        assertEquals(195L, dspFloor);
    }
}
