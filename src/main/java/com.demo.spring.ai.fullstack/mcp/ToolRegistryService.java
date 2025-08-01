package com.demo.spring.ai.fullstack.mcp;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ToolRegistryService {

    private final ToolCallbackProvider toolCallbackProvider;

    @Autowired(required = false)
    @Qualifier("agent2mcpConfig")
    private Map<String, MultiAgentMcpAssignProperties.McpServerConfig> mcpAgentConfigs;


    public ToolRegistryService(@Qualifier("innerMcpToolCallBackProvider") ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public List<ToolDescription> getRegisteredTools() {
        List<ToolDescription> result = new ArrayList<>();
        if (mcpAgentConfigs != null) {
            for (Map.Entry<String, MultiAgentMcpAssignProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
                MultiAgentMcpAssignProperties.McpServerConfig config = entry.getValue();
                for (MultiAgentMcpAssignProperties.McpServerInfo serverInfo : config.mcpServers()) {
                    if (!serverInfo.enabled()) {
                        continue;
                    }
                    result.add(new ToolDescription(serverInfo.description(), serverInfo.description(), null));
                }
            }
        }
        List<ToolDescription> innerTool = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(this::convertToToolDescription)
                .toList();
        result.addAll(innerTool);
        return result;
    }

    private ToolDescription convertToToolDescription(ToolCallback callback) {
        ToolDefinition toolDefinition = callback.getToolDefinition();
        return new ToolDescription(
                toolDefinition.name(),
                toolDefinition.description(),
                toolDefinition.inputSchema()
        );
    }

    public record ToolDescription(String name, String description, String inputSchema) {
    }
}
