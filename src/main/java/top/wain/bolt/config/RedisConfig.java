package top.wain.bolt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import top.wain.bolt.cache.CacheInvalidationListener;

/**
 * @Description: Redis 配置，注册 Pub/Sub 监听容器
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@Configuration
public class RedisConfig {

    @Value("${bolt.cache.invalidation-channel:bolt:cache:invalidate}")
    private String invalidationChannel;

    @Bean
    public ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic(invalidationChannel);
    }

    @Bean
    public MessageListenerAdapter cacheListenerAdapter(CacheInvalidationListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter cacheListenerAdapter,
            ChannelTopic cacheInvalidationTopic) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheListenerAdapter, cacheInvalidationTopic);
        return container;
    }
}
