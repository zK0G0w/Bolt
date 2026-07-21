package top.wain.bolt.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Description: 基于 Redis + Caffeine 的广告源仓储实现，read-through 模式
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class RedisAdSourceRepository implements AdSourceRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisAdSourceRepository.class);
    private static final String KEY_PREFIX = "bolt:adsource:";
    private static final String INDEX_PREFIX = "bolt:adsource:index:";

    private final Cache<String, AdSource> entityCache;
    private final Cache<String, List<String>> indexCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAdSourceRepository(Cache<String, AdSource> adSourceCache,
                                   Cache<String, List<String>> adSourceIndexCache,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper) {
        this.entityCache = adSourceCache;
        this.indexCache = adSourceIndexCache;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AdSource> findByAdPositionId(String adPositionId) {
        List<String> sourceIds = indexCache.get(adPositionId, this::loadIndex);
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        return sourceIds.stream()
                .map(id -> findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<AdSource> findById(String sourceId) {
        AdSource source = entityCache.get(sourceId, this::loadEntity);
        return Optional.ofNullable(source);
    }

    private List<String> loadIndex(String adPositionId) {
        String json = redisTemplate.opsForValue().get(INDEX_PREFIX + adPositionId);
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("反序列化广告源索引失败, posId={}", adPositionId, e);
            return List.of();
        }
    }

    private AdSource loadEntity(String sourceId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + sourceId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, AdSource.class);
        } catch (Exception e) {
            log.error("反序列化广告源失败, sourceId={}", sourceId, e);
            return null;
        }
    }
}

