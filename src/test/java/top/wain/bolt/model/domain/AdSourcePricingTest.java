package top.wain.bolt.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Description: AdSource 定价行为测试，覆盖提交底价 / 准入底价 / 结算价三个口径
 * @Author: WainZeng
 * @Date: 2026/07/27
 */
class AdSourcePricingTest {

    private AdSource.RtbSource rtb(long bidFloor, int profitRatio, AdSource.PriceMarkup markup) {
        return new AdSource.RtbSource("src-1", "pos-1", "plat-1", "slot-1", 200, bidFloor, profitRatio, markup);
    }

    @Test
    void rtbSource_submitPrice_appliesRatioMarkup() {
        // 100 * (100 + 20) / 100 = 120
        assertEquals(120L, rtb(100L, 10, new AdSource.PriceMarkup.Ratio(20)).submitPrice());
    }

    @Test
    void rtbSource_submitPrice_fixedMarkupIgnoresBidFloor() {
        assertEquals(350L, rtb(200L, 10, new AdSource.PriceMarkup.Fixed(350L)).submitPrice());
    }

    @Test
    void rtbSource_floorPrice_isBidFloor() {
        // 准入底价用原始底价，不含加价
        assertEquals(200L, rtb(200L, 10, new AdSource.PriceMarkup.Ratio(50)).floorPrice());
    }

    @Test
    void rtbSource_settlePrice_deductsProfitRatio() {
        // 500 * (100 - 20) / 100 = 400
        assertEquals(400L, rtb(100L, 20, new AdSource.PriceMarkup.Ratio(0)).settlePrice(500L));
    }

    @Test
    void fixedPriceSource_allPricesDeriveFromFixedBidPrice() {
        AdSource fixed = new AdSource.FixedPriceSource("src-2", "pos-1", "plat-1", "slot-1", 300, 500L);

        assertEquals(500L, fixed.submitPrice());
        assertEquals(500L, fixed.floorPrice());
        // 固定价模式不抽利润
        assertEquals(600L, fixed.settlePrice(600L));
    }

    @Test
    void priceMarkup_ratioZero_keepsBase() {
        assertEquals(150L, new AdSource.PriceMarkup.Ratio(0).apply(150L));
    }

    @Test
    void priceMarkup_negativeRatio_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new AdSource.PriceMarkup.Ratio(-1));
    }
}
