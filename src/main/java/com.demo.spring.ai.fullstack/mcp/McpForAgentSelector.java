package com.demo.spring.ai.fullstack.mcp;

import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * agent mcp选择器
 */
@Configuration
public class McpForAgentSelector {

    @Autowired(required = false)
    private Map<String, AsyncMcpToolCallbackProvider> agent2AsyncMcpToolCallbackProvider;

    @Autowired(required = false)
    private Map<String, SyncMcpToolCallbackProvider> agent2SyncMcpToolCallbackProvider;

    /**
     * 获取指定代理的MCP工具回调
     */
    public ToolCallback[] getMcpToolCallbacks(String agentName) {
        if (CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)
                && CollectionUtils.isEmpty(agent2AsyncMcpToolCallbackProvider)) {
            return new ToolCallback[0];
        }
        if (!CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)) {
            SyncMcpToolCallbackProvider toolCallbackProvider = agent2SyncMcpToolCallbackProvider.get(agentName);
            return toolCallbackProvider.getToolCallbacks();
        } else {
            AsyncMcpToolCallbackProvider toolCallbackProvider = agent2AsyncMcpToolCallbackProvider.get(agentName);
            return toolCallbackProvider.getToolCallbacks();
        }
    }
}
