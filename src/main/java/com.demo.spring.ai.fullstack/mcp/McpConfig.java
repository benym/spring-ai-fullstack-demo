package com.demo.spring.ai.fullstack.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean(name = "innerMcpToolCallBackProvider")
    public ToolCallbackProvider mcpTools(TimeService timeService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(timeService)
                .build();
    }
}
