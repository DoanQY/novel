package io.github.xxyopen.novel.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.xxyopen.novel.core.constant.CacheConsts;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 缓存配置类
 *
 * @author xiongxiaoyang
 * @date 2022/5/12
 */
@Configuration
public class CacheConfig {

    /**
     * Caffeine 缓存管理器
     */
    @Bean // 该方法返回一个bean
    @Primary // 优先使用这个缓存管理器
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches = new ArrayList<>(CacheConsts.CacheEnum.values().length);
        // 类型推断 var 非常适合 for 循环，JDK 10 引入，JDK 11 改进
        for (var c : CacheConsts.CacheEnum.values()) {
            if (c.isLocal()) {
                // recordStats() 开启统计功能, 如命中次数、缺失次数、加载次数、回收次数等
                Caffeine<Object, Object> caffeine = Caffeine.newBuilder().recordStats()
                    .maximumSize(c.getMaxSize());
                if (c.getTtl() > 0) {
                    caffeine.expireAfterWrite(Duration.ofSeconds(c.getTtl()));
                }
                caches.add(new CaffeineCache(c.getName(), caffeine.build()));
            }
        }

        cacheManager.setCaches(caches);
        return cacheManager;
    }

    /**
     * Redis 缓存管理器
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 非锁定的 Redis 缓存写入器，多个线程可以同时进行缓存写入操作，而不会出现锁定问题
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
            connectionFactory);
        // 默认配置，过期时间 0 表示永不过期
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues().prefixCacheNameWith(CacheConsts.REDIS_CACHE_PREFIX); // 禁用了对空值的缓存并设置了缓存的前缀

        Map<String, RedisCacheConfiguration> cacheMap = new LinkedHashMap<>(
            CacheConsts.CacheEnum.values().length);

        for (var c : CacheConsts.CacheEnum.values()) {
            if (c.isRemote()) {
                if (c.getTtl() > 0) {
                    cacheMap.put(c.getName(),
                        RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()
                            .prefixCacheNameWith(CacheConsts.REDIS_CACHE_PREFIX)
                            .entryTtl(Duration.ofSeconds(c.getTtl())));
                } else {
                    cacheMap.put(c.getName(),
                        RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()
                            .prefixCacheNameWith(CacheConsts.REDIS_CACHE_PREFIX));
                }
            }
        }

        RedisCacheManager redisCacheManager = new RedisCacheManager(redisCacheWriter,
            defaultCacheConfig, cacheMap);
        // 设置了redisCacheManager是否具有事务感知能力，如果启用事务，则缓存的清除将延迟到事务提交时
        redisCacheManager.setTransactionAware(true);
        // 对缓存管理器进行初始化
        redisCacheManager.initializeCaches();
        return redisCacheManager;
    }

}
