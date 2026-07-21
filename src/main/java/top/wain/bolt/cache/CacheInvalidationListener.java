package top.wain.bolt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import top.wain.bolt.repository.RedisAdSourceRepository;
import top.wain.bolt.repository.RedisDspPlatformRepository;

/**
 * @Description: Redis Pub/Sub 缓存失效监听器，按 entity 类型路由到对应 ManagedCache
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Component
public class CacheInvalidationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final RedisAdSourceRepository adSourceRepository;
    private final RedisDspPlatformRepository dspPlatformRepository;
    private final ObjectMapper objectMapper;

    public CacheInvalidationListener(RedisAdSourceRepository adSourceRepository,
                                     RedisDspPlatformRepository dspPlatformRepository,
                                     ObjectMapper objectMapper) {
        this.adSourceRepository = adSourceRepository;
        this.dspPlatformRepository = dspPlatformRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            var msg = objectMapper.readValue(message.getBody(), CacheInvalidationMessage.class);
            switch (msg.entity()) {
                case "adsource" -> {
                    adSourceRepository.entityCache().invalidate(msg.id());
                    adSourceRepository.indexCache().invalidateAll();
                }
                case "dsp" -> dspPlatformRepository.cache().invalidate(msg.id());
                default -> log.warn("未知的缓存失效实体类型: {}", msg.entity());
            }
        } catch (Exception e) {
            log.error("处理缓存失效消息失败: {}", new String(message.getBody()), e);
        }
    }
}
