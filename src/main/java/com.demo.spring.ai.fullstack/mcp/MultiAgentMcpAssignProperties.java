package com.demo.spring.ai.fullstack.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = MultiAgentMcpAssignProperties.MCP_ASSIGN_PROPERTIES_PREFIX)
public class MultiAgentMcpAssignProperties {

    public static final String MCP_ASSIGN_PROPERTIES_PREFIX = "demo.ai.mcp.assign";

    /**
     * 是否启用mcp节点分配
     */
    private boolean enabled = true;

    /**
     * mcp配置文件位置
     */
    private String mcpConfigLocation = "classpath:mcp/mcp-config.json";

    /**
     * MCP服务器配置
     */
    public record McpServerConfig(@JsonProperty("mcp-servers") List<McpServerInfo> mcpServers) {
    }

    /**
     * MCP服务器信息
     */
    public record McpServerInfo(String url, String sseEndpoint, String description, boolean enabled) {
    }
}
