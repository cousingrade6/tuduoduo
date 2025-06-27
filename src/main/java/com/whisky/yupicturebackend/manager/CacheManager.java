package com.whisky.yupicturebackend.manager;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.util.DigestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CacheManager {

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L) // 最大 10000 条
            // 缓存 5 分钟后移除
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    private final StringRedisTemplate stringRedisTemplate;

    public CacheManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 从缓存中获取数据（多级缓存）
     * @param cacheKey 缓存键
     * @param clazz 返回类型
     * @param <T> 泛型
     * @return 缓存数据或null
     */
    public <T> T getFromCache(String cacheKey, Class<T> clazz) {
        // 1. 先从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, clazz);
        }

        // 2. 本地缓存未命中，查询Redis分布式缓存
        cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            // 更新本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return JSONUtil.toBean(cachedValue, clazz);
        }

        return null;
    }

    /**
     * 设置缓存（多级缓存）
     * @param cacheKey 缓存键
     * @param value 缓存值
     * @param baseExpireTime 基础过期时间（秒）
     * @param randomRange 随机范围（秒）
     */
    public void setCache(String cacheKey, Object value, long baseExpireTime, long randomRange) {
        String cacheValue = JSONUtil.toJsonStr(value);

        // 设置Redis缓存（带随机过期时间防止缓存雪崩）
        long expireTime = baseExpireTime + ThreadLocalRandom.current().nextLong(randomRange);
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                cacheValue,
                expireTime,
                TimeUnit.SECONDS
        );

        // 设置本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
    }

    /**
     * 生成缓存键
     * @param prefix 缓存前缀
     * @param queryCondition 查询条件
     * @return 完整的缓存键
     */
    public String generateCacheKey(String prefix, Object queryCondition) {
        String queryStr = JSONUtil.toJsonStr(queryCondition);
        String hashKey = DigestUtils.md5DigestAsHex(queryStr.getBytes());
        return String.format("%s:%s", prefix, hashKey);
    }


}
