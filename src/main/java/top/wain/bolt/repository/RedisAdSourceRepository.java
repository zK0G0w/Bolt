package top.wain.bolt.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.cache.ManagedCache;
import top.wain.bolt.model.domain.AdSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @Description: 基于 Redis + Caffeine 的广告源仓储实现，通过 ManagedCache 统一管理缓存生命周期
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class RedisAdSourceRepository implements AdSourceRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisAdSourceRepository.class);
    private static final String KEY_PREFIX = "bolt:adsource:";
    private static final String INDEX_PREFIX = "bolt:adsource:index:";

    private final ManagedCache<String, AdSource> entityCache;
    private final ManagedCache<String, List<String>> indexCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAdSourceRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.entityCache = ManagedCache.<String, AdSource>builder()
                .name("adsource")
                .loader(this::loadEntity)
                .build();
        this.indexCache = ManagedCache.<String, List<String>>builder()
                .name("adsource-index")
                .loader(this::loadIndex)
                .build();
    }

    @PostConstruct
    void warmUp() {
        entityCache.warmUp(this::warmEntities);
        indexCache.warmUp(this::warmIndices);
    }

    public ManagedCache<String, AdSource> entityCache() {
        return entityCache;
    }

    public ManagedCache<String, List<String>> indexCache() {
        return indexCache;
    }

    @Override
    public List<AdSource> findByAdPositionId(String adPositionId) {
        List<String> sourceIds = indexCache.get(adPositionId);
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
        AdSource source = entityCache.get(sourceId);
        return Optional.ofNullable(source);
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

    private Collection<ManagedCache.Entry<String, AdSource>> warmEntities() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return List.of();
        List<ManagedCache.Entry<String, AdSource>> entries = new ArrayList<>();
        for (String key : keys) {
            if (key.startsWith(INDEX_PREFIX)) continue;
            String sourceId = key.substring(KEY_PREFIX.length());
            AdSource source = loadEntity(sourceId);
            if (source != null) {
                entries.add(new ManagedCache.Entry<>(sourceId, source));
            }
        }
        return entries;
    }

    private Collection<ManagedCache.Entry<String, List<String>>> warmIndices() {
        Set<String> keys = redisTemplate.keys(INDEX_PREFIX + "*");
        if (keys == null) return List.of();
        List<ManagedCache.Entry<String, List<String>>> entries = new ArrayList<>();
        for (String key : keys) {
            String posId = key.substring(INDEX_PREFIX.length());
            List<String> ids = loadIndex(posId);
            if (!ids.isEmpty()) {
                entries.add(new ManagedCache.Entry<>(posId, ids));
            }
        }
        return entries;
    }
}