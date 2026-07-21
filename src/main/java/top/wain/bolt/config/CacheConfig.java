package top.wain.bolt.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.time.Duration;
import java.util.List;

/**
 * @Description: Caffeine 本地缓存配置，所有缓存实例 5 分钟 TTL 兜底
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Configuration
public class CacheConfig {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_SIZE = 10_000;

    @Bean
    public Cache<String, AdSource> adSourceCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .build();
    }

    @Bean
    public Cache<String, List<String>> adSourceIndexCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .build();
    }

    @Bean
    public Cache<String, DspPlatform> dspPlatformCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE)
                .build();
    }
}
