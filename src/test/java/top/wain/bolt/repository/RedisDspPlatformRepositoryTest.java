package top.wain.bolt.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.wain.bolt.model.domain.DspPlatform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisDspPlatformRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisDspPlatformRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper();
        repository = new RedisDspPlatformRepository(redisTemplate, objectMapper);
    }

    @Test
    void findById_cacheMiss_loadsFromRedis() throws Exception {
        var platform = new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        when(valueOps.get("bolt:dsp:plat-001")).thenReturn(objectMapper.writeValueAsString(platform));

        var result = repository.findById("plat-001");

        assertTrue(result.isPresent());
        assertEquals("华为ADX", result.get().name());
    }

    @Test
    void findById_cacheHit_doesNotHitRedis() {
        var platform = new DspPlatform("plat-001", "华为ADX", "huawei", "https://adx.huawei.com/bid", 1000, 50);
        repository.cache().put("plat-001", platform);

        var result = repository.findById("plat-001");

        assertTrue(result.isPresent());
        verify(valueOps, never()).get(anyString());
    }

    @Test
    void findById_notInRedis_returnsEmpty() {
        when(valueOps.get("bolt:dsp:plat-999")).thenReturn(null);
        var result = repository.findById("plat-999");
        assertTrue(result.isEmpty());
    }
}
