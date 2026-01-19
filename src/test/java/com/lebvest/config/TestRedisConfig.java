package com.lebvest.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Test configuration that provides mock Redis beans for integration tests
 * when Redis is not available in the CI environment
 */
@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory() {
        // Return a mock connection factory that doesn't actually connect
        // This is a minimal implementation that allows the context to load
        return org.mockito.Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // Return a mock RedisTemplate that doesn't actually connect to Redis
        // This allows the application context to load without a real Redis connection
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        // Use NoOpCacheManager for tests - caching is disabled
        return new NoOpCacheManager();
    }
}

