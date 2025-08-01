package com.demo.spring.ai.fullstack.memory;

import com.alibaba.cloud.ai.memory.redis.serializer.MessageDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.redisson.Redisson;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.config.Config;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import java.util.List;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.util.Assert;
import java.util.stream.StreamSupport;
import java.util.*;
import java.util.stream.Collectors;

public class RedissonChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RedissonChatMemoryRepository.class);
    private static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory:";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private RedissonChatMemoryRepository(RedissonClient redissonClient) {
        Assert.notNull(redissonClient, "redissonClient cannot be null");
        this.redissonClient = redissonClient;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Message.class, new MessageDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    public static RedissonBuilder builder() {
        return new RedissonBuilder();
    }

    public static class RedissonBuilder {
        private String host = "127.0.0.1";

        private int port = 6379;

        private String password;

        private int timeout = 3000;

        private int poolSize = 32;

        private RedisClusterConfig clusterConfig;

        private boolean useCluster = false;

        public RedissonBuilder host(String host) {
            this.host = host;
            return this;
        }

        public RedissonBuilder port(int port) {
            this.port = port;
            return this;
        }

        public RedissonBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RedissonBuilder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public RedissonBuilder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public RedissonBuilder useCluster(RedisClusterConfig clusterConfig) {
            this.clusterConfig = clusterConfig;
            this.useCluster = true;
            return this;
        }

        public RedissonChatMemoryRepository build() {
            Config config = new Config();
            if (useCluster) {
                config.useClusterServers()
                        .addNodeAddress(clusterConfig.getNodeAddresses().toArray(new String[0]))
                        .setPassword(password)
                        .setConnectTimeout(timeout)
                        .setSlaveConnectionPoolSize(poolSize)
                        .setMasterConnectionPoolSize(poolSize);
            } else {
                config.useSingleServer()
                        .setAddress("redis://" + host + ":" + port)
                        .setPassword(password)
                        .setConnectionPoolSize(poolSize)
                        .setConnectTimeout(timeout);
            }
            return new RedissonChatMemoryRepository(Redisson.create(config));
        }
    }

    public static class RedisClusterConfig {

        private final List<String> nodeAddresses;

        public RedisClusterConfig(List<String> nodeAddresses) {
            Assert.notEmpty(nodeAddresses, "Cluster nodes cannot be empty");
            this.nodeAddresses = new ArrayList<>(nodeAddresses);
        }

        public List<String> getNodeAddresses() {
            return Collections.unmodifiableList(nodeAddresses);
        }
    }

    @NotNull
    @Override
    public List<String> findConversationIds() {
        RKeys keys = redissonClient.getKeys();
        // 使用推荐的ScanOptions替代已过时的getKeysByPattern
        KeysScanOptions scanOptions = KeysScanOptions.defaults()
                .pattern(DEFAULT_KEY_PREFIX + "*");
        Iterable<String> keysIter = keys.getKeys(scanOptions);
        // 使用Stream处理避免大keys列表OOM
        return StreamSupport.stream(keysIter.spliterator(), false)
                .map(key -> key.substring(DEFAULT_KEY_PREFIX.length()))
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        RList<String> redisList = redissonClient.getList(
                DEFAULT_KEY_PREFIX + conversationId
        );
        return redisList.readAll()
                .parallelStream()
                .map(this::deserializeMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Message deserializeMessage(String messageStr) {
        try {
            return objectMapper.readValue(messageStr, Message.class);
        } catch (JsonProcessingException e) {
            logger.error("Deserialization error for message: {}", truncate(messageStr, 100), e);
            return null;
        }
    }

    private String truncate(String str, int maxLength) {
        return str.length() > maxLength
                ? str.substring(0, maxLength) + "..."
                : str;
    }

    @Override
    public void saveAll(@NotNull String conversationId, @NotNull List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");
        RList<String> redisList = redissonClient.getList(
                DEFAULT_KEY_PREFIX + conversationId
        );
        // 删除现有对话消息（原子操作）
        redisList.delete();
        // 批量添加所有消息（高效批处理）
        List<String> serializedMessages = messages.stream()
                .map(this::serializeMessage)
                .toList();
        redisList.addAll(serializedMessages);
    }

    private String serializeMessage(Message message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialization error for message: " + message, e);
        }
    }

    @Override
    public void deleteByConversationId(@NotNull String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        RList<String> redisList = redissonClient.getList(
                DEFAULT_KEY_PREFIX + conversationId
        );
        redisList.delete();
    }

    @Override
    public void close() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            try {
                int activeConnections = redissonClient.getConfig().getNettyThreads();
                logger.info("Shutting down Redisson with {} active connections", activeConnections);
                redissonClient.shutdown();
                logger.info("Redisson client shutdown completed");
            } catch (Exception e) {
                logger.error("Error shutting down Redisson client", e);
            }
        }
    }
}
