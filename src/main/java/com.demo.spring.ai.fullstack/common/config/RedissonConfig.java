package com.demo.spring.ai.fullstack.common.config;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.demo.spring.ai.fullstack.memory.RedissonChatMemoryRepository;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;


@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.cluster.nodes}")
    private String redisCluster;

    @Value("${spring.data.redis.password}")
    private String passWord;

    @Value("${spring.ai.memory.redis.host}")
    private String redisHost;

    @Value("${spring.ai.memory.redis.port}")
    private int redisPort;

    @Value("${spring.ai.memory.redis.password}")
    private String redisPassword;

    @Value("${spring.ai.memory.redis.timeout}")
    private int redisTimeout;

    @Bean
    public RedissonClient getRedissonClient() {
        Config config = new Config();
        if (StringUtils.hasLength(redisCluster)) {
            //集群模式配置
            String[] nodes = redisCluster.split(",");
            List<String> clusterNodes = new ArrayList<>();
            for (String node : nodes) {
                clusterNodes.add("redis://" + node);
            }
            config.useClusterServers()
                    .addNodeAddress(clusterNodes.toArray(new String[0]))
                    .setCheckSlotsCoverage(false);
            if (StringUtils.hasLength(passWord)) {
                config.useClusterServers().setPassword(passWord);
            }
        }
        return Redisson.create(config);
    }

    /**
     * 将value存储为json
     *
     * @param redisConnectionFactory redisConnectionFactory，spring data redis自动注入
     * @return RedisTemplate<String, Object>
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 注入数据源
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 指定objectMapper带输入类型的序列化，如果不指定redis中则存储纯json，序列化返回后解析默认为LinkedHashMap
        // 需要自己转换类型，指定序列化类型后无需再进行转化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        // 处理LocalDateTime
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        // key-value结构序列化数据结构
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        // hash数据结构序列化方式
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        // 启用默认序列化方式
        redisTemplate.setEnableDefaultSerializer(true);
        redisTemplate.setDefaultSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 将value存储为string
     *
     * @param redisConnectionFactory redisConnectionFactory，spring data redis自动注入
     * @return RedisTemplate<String, Object>
     */
    @Bean(name = "redisTemplateString")
    public RedisTemplate<String, String> redisTemplateString(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 指定objectMapper带输入类型的序列化，如果不指定redis中则存储纯json，序列化返回后解析默认为LinkedHashMap
        // 需要自己转换类型，指定序列化类型后无需再进行转化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        // 处理LocalDateTime
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer<String> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, String.class);
        // key-value结构序列化数据结构
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        // hash数据结构序列化方式
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        // 启用默认序列化方式
        redisTemplate.setEnableDefaultSerializer(true);
        redisTemplate.setDefaultSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return RedisChatMemoryRepository.builder()
                .host(redisHost)
                .port(redisPort)
				.password(redisPassword)
                .timeout(redisTimeout)
                .build();
    }

    @Bean
    public RedissonChatMemoryRepository redissonChatMemoryRepository() {
        String[] nodes = redisCluster.split(",");
        List<String> clusterNodes = new ArrayList<>();
        for (String node : nodes) {
            clusterNodes.add("redis://" + node);
        }
        return RedissonChatMemoryRepository.builder()
                .useCluster(new RedissonChatMemoryRepository.RedisClusterConfig(clusterNodes))
                .host(redisHost)
                .port(redisPort)
                .password(redisPassword)
                .timeout(redisTimeout)
                .build();
    }
}
