package top.wain.bolt.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.Optional;

/**
 * @Description: 基于 Redis + Caffeine 的 DSP 平台配置仓储实现，read-through 模式
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class RedisDspPlatformRepository implements DspPlatformRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisDspPlatformRepository.class);
    private static final String KEY_PREFIX = "bolt:dsp:";

    private final Cache<String, DspPlatform> cache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDspPlatformRepository(Cache<String, DspPlatform> dspPlatformCache,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.cache = dspPlatformCache;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DspPlatform> findById(String platformId) {
        DspPlatform platform = cache.get(platformId, this::loadFromRedis);
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
}
