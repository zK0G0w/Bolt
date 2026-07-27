package top.wain.bolt.tracking;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Description: TrackingPayload 编解码与时效判定测试
 * @Author: WainZeng
 * @Date: 2026/07/27
 */
class TrackingPayloadTest {

    @Test
    void encodeThenDecode_roundTrips() {
        TrackingPayload origin = new TrackingPayload("bid-1", "src-1", 350L, 1700000000000L, "https://lp.com/a?b=1");

        TrackingPayload decoded = TrackingPayload.decode(origin.encode()).orElseThrow();

        assertEquals(origin, decoded);
    }

    @Test
    void decode_emptyLandingUrl_preserved() {
        // split 需保留末尾空串，否则展示链接会被判为字段缺失
        TrackingPayload decoded = TrackingPayload.decode(
                new TrackingPayload("bid-2", "src-2", 100L, 1L, "").encode()).orElseThrow();

        assertEquals("", decoded.landingUrl());
        assertFalse(decoded.hasLandingUrl());
    }

    @Test
    void decode_landingUrlContainsSeparator_notTruncated() {
        // limit=5 保证落地页里的 | 不会被继续切分
        String landing = "https://lp.com/x?a=1|b=2";
        TrackingPayload decoded = TrackingPayload.decode(
                new TrackingPayload("bid-3", "src-3", 100L, 1L, landing).encode()).orElseThrow();

        assertEquals(landing, decoded.landingUrl());
    }

    @Test
    void decode_missingFields_returnsEmpty() {
        assertEquals(Optional.empty(), TrackingPayload.decode("bid-4|src-4|100|123"));
    }

    @Test
    void decode_nonNumericPrice_returnsEmpty() {
        // 原实现在此处抛 NumberFormatException 导致 500
        assertEquals(Optional.empty(), TrackingPayload.decode("bid-5|src-5|abc|123|"));
    }

    @Test
    void decode_null_returnsEmpty() {
        assertEquals(Optional.empty(), TrackingPayload.decode(null));
    }

    @Test
    void isExpired_reflectsTimestampAge() {
        long now = System.currentTimeMillis();

        assertFalse(new TrackingPayload("b", "s", 1L, now, "").isExpired(60_000L));
        assertTrue(new TrackingPayload("b", "s", 1L, now - 120_000L, "").isExpired(60_000L));
    }
}
