package com.demo.spring.ai.fullstack.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MemoryConfig {

    private final int MAX_MESSAGES = 100;

    @Bean("messageMemory")
    public MessageWindowChatMemory messageWindowChatMemory(RedissonChatMemoryRepository redissonChatMemoryRepository) {
        log.info("Initializing Redis MessageWindowChatMemory with max messages: {}", MAX_MESSAGES);
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redissonChatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

//    @Bean("messageMemory")
//    public MessageWindowChatMemory messageWindowChatMemory() {
//        log.info("Initializing InMemory MessageWindowChatMemory with max messages: {}", MAX_MESSAGES);
//        InMemoryChatMemoryRepository inMemoryChatMemoryRepository = new InMemoryChatMemoryRepository();
//        return MessageWindowChatMemory.builder()
//                .chatMemoryRepository(inMemoryChatMemoryRepository)
//                .maxMessages(MAX_MESSAGES)
//                .build();
//    }
}
