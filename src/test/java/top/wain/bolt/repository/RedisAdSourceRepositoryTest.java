package top.wain.bolt.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.wain.bolt.model.domain.AdSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisAdSourceRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private Cache<String, AdSource> entityCache;
    private Cache<String, List<String>> indexCache;
    private RedisAdSourceRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper();
        entityCache = Caffeine.newBuilder().build();
        indexCache = Caffeine.newBuilder().build();
        repository = new RedisAdSourceRepository(entityCache, indexCache, redisTemplate, objectMapper);
    }

    @Test
    void findById_cacheMiss_loadsFromRedis() throws Exception {
        var source = new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20));
        String json = objectMapper.writeValueAsString(source);
        when(valueOps.get("bolt:adsource:src-001")).thenReturn(json);

        var result = repository.findById("src-001");

        assertTrue(result.isPresent());
        assertEquals("src-001", result.get().sourceId());
        verify(valueOps).get("bolt:adsource:src-001");
    }

    @Test
    void findById_cacheHit_doesNotHitRedis() throws Exception {
        var source = new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20));
        entityCache.put("src-001", source);

        var result = repository.findById("src-001");

        assertTrue(result.isPresent());
        verify(valueOps, never()).get(anyString());
    }

    @Test
    void findById_notInRedis_returnsEmpty() {
        when(valueOps.get("bolt:adsource:src-999")).thenReturn(null);

        var result = repository.findById("src-999");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAdPositionId_loadsIndexThenEntities() throws Exception {
        when(valueOps.get("bolt:adsource:index:imp-001")).thenReturn("[\"src-001\",\"src-002\"]");

        var s1 = new AdSource.RtbSource("src-001", "imp-001", "plat-001", "slot-001", 200, 150L, 15, new AdSource.PriceMarkup.Ratio(20));
        var s2 = new AdSource.FixedPriceSource("src-002", "imp-001", "plat-002", "slot-002", 300, 500L);
        when(valueOps.get("bolt:adsource:src-001")).thenReturn(objectMapper.writeValueAsString(s1));
        when(valueOps.get("bolt:adsource:src-002")).thenReturn(objectMapper.writeValueAsString(s2));

        List<AdSource> results = repository.findByAdPositionId("imp-001");

        assertEquals(2, results.size());
    }
}

