package top.wain.bolt.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.cache.ManagedCache;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @Description: 基于 Redis + Caffeine 的 DSP 平台配置仓储实现，通过 ManagedCache 统一管理缓存生命周期
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class RedisDspPlatformRepository implements DspPlatformRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisDspPlatformRepository.class);
    private static final String KEY_PREFIX = "bolt:dsp:";

    private final ManagedCache<String, DspPlatform> cache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDspPlatformRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cache = ManagedCache.<String, DspPlatform>builder()
                .name("dsp-platform")
                .loader(this::loadFromRedis)
                .build();
    }

    @PostConstruct
    void warmUp() {
        cache.warmUp(this::warmPlatforms);
    }

    public ManagedCache<String, DspPlatform> cache() {
        return cache;
    }

    @Override
    public Optional<DspPlatform> findById(String platformId) {
        DspPlatform platform = cache.get(platformId);
        return Optional.ofNullable(platform);
    }

    private DspPlatform loadFromRedis(String platformId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + platformId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, DspPlatform.class);
        } catch (Exception e) {
            log.error("反序列化DSP平台失败, platformId={}", platformId, e);
            return null;
        }
    }

    private Collection<ManagedCache.Entry<String, DspPlatform>> warmPlatforms() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return List.of();
        List<ManagedCache.Entry<String, DspPlatform>> entries = new ArrayList<>();
        for (String key : keys) {
            String platformId = key.substring(KEY_PREFIX.length());
            DspPlatform platform = loadFromRedis(platformId);
            if (platform != null) {
                entries.add(new ManagedCache.Entry<>(platformId, platform));
            }
        }
        return entries;
    }
}