package top.wain.bolt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @Description: 托管缓存，将 Caffeine 缓存的创建、read-through、预热、失效收拢在一个模块内
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
public class ManagedCache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(ManagedCache.class);

    private final String name;
    private final Cache<K, V> cache;
    private final Function<K, V> loader;

    private ManagedCache(String name, Cache<K, V> cache, Function<K, V> loader) {
        this.name = name;
        this.cache = cache;
        this.loader = loader;
    }

    public V get(K key) {
        return cache.get(key, loader);
    }

    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public void invalidate(K key) {
        cache.invalidate(key);
        log.info("缓存失效: cache={} key={}", name, key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
        log.info("缓存全量失效: cache={}", name);
    }

    public String name() {
        return name;
    }

    /**
     * 执行预热：由外部提供预热数据源，批量灌入缓存
     */
    public void warmUp(Supplier<Collection<Entry<K, V>>> warmupSource) {
        Collection<Entry<K, V>> entries = warmupSource.get();
        for (Entry<K, V> entry : entries) {
            cache.put(entry.key(), entry.value());
        }
        log.info("缓存预热完成: cache={} count={}", name, entries.size());
    }

    public record Entry<K, V>(K key, V value) {}

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {
        private String name;
        private Duration ttl = Duration.ofMinutes(5);
        private int maxSize = 10_000;
        private Function<K, V> loader;

        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<K, V> ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder<K, V> loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        public ManagedCache<K, V> build() {
            Cache<K, V> cache = Caffeine.newBuilder()
                    .expireAfterWrite(ttl)
                    .maximumSize(maxSize)
                    .build();
            return new ManagedCache<>(name, cache, loader);
        }
    }
}