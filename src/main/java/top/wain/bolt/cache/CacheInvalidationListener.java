package top.wain.bolt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import top.wain.bolt.model.domain.AdSource;
import top.wain.bolt.model.domain.DspPlatform;

import java.util.List;

/**
 * @Description: Redis Pub/Sub 缓存失效监听器，收到消息后 evict 对应 Caffeine 条目
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class CacheInvalidationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final Cache<String, AdSource> adSourceCache;
    private final Cache<String, List<String>> adSourceIndexCache;
    private final Cache<String, DspPlatform> dspPlatformCache;
    private final ObjectMapper objectMapper;

    public CacheInvalidationListener(Cache<String, AdSource> adSourceCache,
                                     Cache<String, List<String>> adSourceIndexCache,
                                     Cache<String, DspPlatform> dspPlatformCache,
                                     ObjectMapper objectMapper) {
        this.adSourceCache = adSourceCache;
        this.adSourceIndexCache = adSourceIndexCache;
        this.dspPlatformCache = dspPlatformCache;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            var msg = objectMapper.readValue(message.getBody(), CacheInvalidationMessage.class);
            switch (msg.entity()) {
                case "adsource" -> {
                    adSourceCache.invalidate(msg.id());
                    // 索引缓存整体失效，下次访问时回源重建
                    adSourceIndexCache.invalidateAll();
                    log.info("缓存失效: adsource id={} action={}", msg.id(), msg.action());
                }
                case "dsp" -> {
                    dspPlatformCache.invalidate(msg.id());
                    log.info("缓存失效: dsp id={} action={}", msg.id(), msg.action());
                }
                default -> log.warn("未知的缓存失效实体类型: {}", msg.entity());
            }
        } catch (Exception e) {
            log.error("处理缓存失效消息失败: {}", new String(message.getBody()), e);
        }
    }
}
