package top.wain.bolt.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.List;
import java.util.Set;

/**
 * @Description: 启动时从 Redis 全量预热本地 Caffeine 缓存
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);
    private static final String AD_SOURCE_PREFIX = "bolt:adsource:";
    private static final String AD_SOURCE_INDEX_PREFIX = "bolt:adsource:index:";
    private static final String DSP_PREFIX = "bolt:dsp:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, AdSource> adSourceCache;
    private final Cache<String, List<String>> adSourceIndexCache;
    private final Cache<String, DspPlatform> dspPlatformCache;

    public CacheWarmer(StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       Cache<String, AdSource> adSourceCache,
                       Cache<String, List<String>> adSourceIndexCache,
                       Cache<String, DspPlatform> dspPlatformCache) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.adSourceCache = adSourceCache;
        this.adSourceIndexCache = adSourceIndexCache;
        this.dspPlatformCache = dspPlatformCache;
    }

    @PostConstruct
    public void warmUp() {
        warmAdSources();
        warmDspPlatforms();
    }

    private void warmAdSources() {
        Set<String> keys = redisTemplate.keys(AD_SOURCE_PREFIX + "*");
        if (keys == null) return;

        int entityCount = 0;
        int indexCount = 0;
        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) continue;

            try {
                if (key.startsWith(AD_SOURCE_INDEX_PREFIX)) {
                    String posId = key.substring(AD_SOURCE_INDEX_PREFIX.length());
                    List<String> ids = objectMapper.readValue(json, new TypeReference<>() {});
                    adSourceIndexCache.put(posId, ids);
                    indexCount++;
                } else {
                    String sourceId = key.substring(AD_SOURCE_PREFIX.length());
                    AdSource source = objectMapper.readValue(json, AdSource.class);
                    adSourceCache.put(sourceId, source);
                    entityCount++;
                }
            } catch (Exception e) {
                log.warn("预热广告源失败, key={}", key, e);
            }
        }
        log.info("缓存预热完成: AdSource={} 条, 索引={} 条", entityCount, indexCount);
    }

    private void warmDspPlatforms() {
        Set<String> keys = redisTemplate.keys(DSP_PREFIX + "*");
        if (keys == null) return;

        int count = 0;
        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) continue;

            try {
                String platformId = key.substring(DSP_PREFIX.length());
                DspPlatform platform = objectMapper.readValue(json, DspPlatform.class);
                dspPlatformCache.put(platformId, platform);
                count++;
            } catch (Exception e) {
                log.warn("预热DSP平台失败, key={}", key, e);
            }
        }
        log.info("缓存预热完成: DspPlatform={} 条", count);
    }
}


