package top.wain.bolt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;
import top.wain.bolt.repository.RedisAdSourceRepository;
import top.wain.bolt.repository.RedisDspPlatformRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheInvalidationListenerTest {

    private RedisAdSourceRepository adSourceRepository;
    private RedisDspPlatformRepository dspPlatformRepository;
    private CacheInvalidationListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        adSourceRepository = new RedisAdSourceRepository(redisTemplate, objectMapper);
        dspPlatformRepository = new RedisDspPlatformRepository(redisTemplate, objectMapper);
        listener = new CacheInvalidationListener(adSourceRepository, dspPlatformRepository, objectMapper);
    }

    @Test
    void onMessage_adsourceUpdate_evictsEntityAndIndex() {
        var source = new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20));
        adSourceRepository.entityCache().put("src-001", source);
        adSourceRepository.indexCache().put("imp-001", List.of("src-001"));

        String payload = "{\"entity\":\"adsource\",\"id\":\"src-001\",\"action\":\"update\"}";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        assertNull(adSourceRepository.entityCache().getIfPresent("src-001"));
        assertNull(adSourceRepository.indexCache().getIfPresent("imp-001"));
    }

    @Test
    void onMessage_dspUpdate_evictsPlatformCache() {
        var platform = new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        dspPlatformRepository.cache().put("plat-001", platform);

        String payload = "{\"entity\":\"dsp\",\"id\":\"plat-001\",\"action\":\"update\"}";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        assertNull(dspPlatformRepository.cache().getIfPresent("plat-001"));
    }

    @Test
    void onMessage_invalidJson_doesNotThrow() {
        String payload = "not-valid-json";
        var message = new DefaultMessage("bolt:cache:invalidate".getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> listener.onMessage(message, null));
    }
}