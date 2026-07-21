package top.wain.bolt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CacheInvalidationListenerTest {

    private Cache<String, AdSource> adSourceCache;
    private Cache<String, List<String>> adSourceIndexCache;
    private Cache<String, DspPlatform> dspPlatformCache;
    private CacheInvalidationListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adSourceCache = Caffeine.newBuilder().build();
        adSourceIndexCache = Caffeine.newBuilder().build();
        dspPlatformCache = Caffeine.newBuilder().build();
        objectMapper = new ObjectMapper();
        listener = new CacheInvalidationListener(adSourceCache, adSourceIndexCache, dspPlatformCache, objectMapper);
    }

    @Test
    void onMessage_adsourceUpdate_evictsEntityAndIndex() {
        var source = new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20));
        adSourceCache.put("src-001", source);
        adSourceIndexCache.put("imp-001", List.of("src-001"));

        String payload = "{\"entity\":\"adsource\",\"id\":\"src-001\",\"action\":\"update\"}";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        assertNull(adSourceCache.getIfPresent("src-001"));
        assertNull(adSourceIndexCache.getIfPresent("imp-001"));
    }

    @Test
    void onMessage_dspUpdate_evictsPlatformCache() {
        var platform = new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        dspPlatformCache.put("plat-001", platform);

        String payload = "{\"entity\":\"dsp\",\"id\":\"plat-001\",\"action\":\"update\"}";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        assertNull(dspPlatformCache.getIfPresent("plat-001"));
    }

    @Test
    void onMessage_invalidJson_doesNotThrow() {
        String payload = "not-valid-json";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> listener.onMessage(message, null));
    }
}

