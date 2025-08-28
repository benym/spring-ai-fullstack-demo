package com.demo.spring.ai.fullstack.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({MemoryProperties.class})
@Configuration(enforceUniqueMethods = false)
@Slf4j
public class MemoryAutoConfiguration {

    private final int MAX_MESSAGES = 100;

    @Bean("messageMemory")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "demo.ai.memory.memory-type", havingValue = "redis", matchIfMissing = true)
    public MessageWindowChatMemory messageRedisWindowChatMemory(RedissonChatMemoryRepository redissonChatMemoryRepository) {
        log.info("Initializing Redis MessageWindowChatMemory with max messages: {}", MAX_MESSAGES);
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redissonChatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    @Bean("messageMemory")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "demo.ai.memory.memory-type", havingValue = "simple", matchIfMissing = true)
    public MessageWindowChatMemory messageInMemoryWindowChatMemory() {
        log.info("Initializing InMemory MessageWindowChatMemory with max messages: {}", MAX_MESSAGES);
        InMemoryChatMemoryRepository inMemoryChatMemoryRepository = new InMemoryChatMemoryRepository();
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(inMemoryChatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
