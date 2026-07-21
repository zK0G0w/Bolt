package top.wain.bolt;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Description: WebMvcTest 共享的 Redis mock 配置，避免测试依赖真实 Redis
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@TestConfiguration
public class MockRedisTestConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @SuppressWarnings("unchecked")
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.keys(anyString())).thenReturn(Set.of());
        when(template.hasKey(anyString())).thenReturn(false);
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        return mock(RedisMessageListenerContainer.class);
    }
}
