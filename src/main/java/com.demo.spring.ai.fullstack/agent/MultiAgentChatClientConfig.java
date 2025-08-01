package com.demo.spring.ai.fullstack.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多Agent注入
 * ChatClient.Builder注入查看
 * @see MultiAgentModelConfig
 */
@Configuration
public class MultiAgentChatClientConfig {

    /**
     * 注入searchAgent
     *
     * @param thinkModelChatClientBuilder 注入名为配置的model-config名+ChatClientBuilder
     * @return ChatClient
     */
    @Bean
    public ChatClient searchAgent(ChatClient.Builder thinkModelChatClientBuilder) {
        return thinkModelChatClientBuilder
                .build();
    }

    /**
     * 注入weatherAgent
     *
     * @param noThinkModelChatClientBuilder 注入名为配置的model-config名+ChatClientBuilder
     * @return ChatClient
     */
    @Bean
    public ChatClient weatherAgent(ChatClient.Builder noThinkModelChatClientBuilder) {
        return noThinkModelChatClientBuilder
                .build();
    }
}
