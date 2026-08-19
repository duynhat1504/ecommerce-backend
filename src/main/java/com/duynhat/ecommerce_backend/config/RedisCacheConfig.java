package com.duynhat.ecommerce_backend.config;

import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String CATEGORY_LIST = "categoryList";

    public static final String CATEGORY_DETAIL = "categoryDetail";

    public static final String PRODUCT_DETAIL = "productDetail";

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {

        JacksonJsonRedisSerializer<CategoryResponse> categoryDetailSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, CategoryResponse.class);

        JavaType categoryListType = objectMapper
                .getTypeFactory()
                .constructCollectionType(
                        List.class,
                        CategoryResponse.class
                        );

        JacksonJsonRedisSerializer<List<CategoryResponse>> categoryListSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, categoryListType);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .disableCachingNullValues();

        RedisCacheConfiguration categoryListConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(categoryListSerializer)
                );

        RedisCacheConfiguration categoryDetailConfig =
                defaultConfig
                        .entryTtl(Duration.ofMinutes(30))
                        .serializeValuesWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(categoryDetailSerializer)
                        );

        JacksonJsonRedisSerializer<ProductResponse> productDetailSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, ProductResponse.class);

        RedisCacheConfiguration productDetailConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(productDetailSerializer)
                );

        Map<String, RedisCacheConfiguration>
                cacheConfigurations =
                Map.of(
                        CATEGORY_LIST,
                        categoryListConfig,

                        CATEGORY_DETAIL,
                        categoryDetailConfig,

                        PRODUCT_DETAIL,
                        productDetailConfig
                );

        RedisCacheWriter cacheWriter =
                RedisCacheWriter.create(
                        connectionFactory,
                        configurer -> configurer.immediateWrites()
                );

        return RedisCacheManager
                .builder(cacheWriter)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}
