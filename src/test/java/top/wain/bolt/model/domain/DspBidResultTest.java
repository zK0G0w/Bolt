package top.wain.bolt.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DspBidResultTest {

    @Test
    void patternMatching_exhaustive_switch() {
        DspBidResult success = new DspBidResult.Success("src-001", 300L, "payload", "raw");
        DspBidResult noBid = new DspBidResult.NoBid("src-002");
        DspBidResult timeout = new DspBidResult.Timeout("src-003");
        DspBidResult error = new DspBidResult.Error("src-004", "connection refused");

        for (DspBidResult result : new DspBidResult[]{success, noBid, timeout, error}) {
            String desc = switch (result) {
                case DspBidResult.Success s -> "出价:" + s.price();
                case DspBidResult.NoBid n -> "不出价";
                case DspBidResult.Timeout t -> "超时";
                case DspBidResult.Error e -> "错误:" + e.reason();
            };
            assertFalse(desc.isEmpty());
            assertNotNull(result.adSourceId());
        }
    }

    @Test
    void success_record_fields() {
        var s = new DspBidResult.Success("src-001", 250L, "ad-payload", "{\"bid\":250}");
        assertEquals("src-001", s.adSourceId());
        assertEquals(250L, s.price());
        assertEquals("ad-payload", s.adPayload());
        assertEquals("{\"bid\":250}", s.rawResponse());
    }

    @Test
    void record_equals_and_hashCode() {
        var a = new DspBidResult.Success("src-001", 300L, "p", "r");
        var b = new DspBidResult.Success("src-001", 300L, "p", "r");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        var e1 = new DspBidResult.Error("src-002", "timeout");
        var e2 = new DspBidResult.Error("src-002", "timeout");
        assertEquals(e1, e2);
    }

    @Test
    void commonAccessor_adSourceId() {
        DspBidResult[] results = {
                new DspBidResult.Success("s1", 100L, "", ""),
                new DspBidResult.NoBid("s2"),
                new DspBidResult.Timeout("s3"),
                new DspBidResult.Error("s4", "err")
        };

        String[] expected = {"s1", "s2", "s3", "s4"};
        for (int i = 0; i < results.length; i++) {
            assertEquals(expected[i], results[i].adSourceId());
        }
    }
}
