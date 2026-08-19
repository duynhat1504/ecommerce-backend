package com.duynhat.ecommerce_backend.modules.product.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.PRODUCT_DETAIL;

@Service
public class ProductCacheService {

    private final CacheManager cacheManager;

    public ProductCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictProductDetails(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        Cache cache = cacheManager.getCache(PRODUCT_DETAIL);

        if (cache == null) {
            throw new IllegalStateException("Product detail cache is not configured");
        }

        productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(cache::evict);
    }
}
