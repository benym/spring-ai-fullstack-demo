package com.demo.spring.ai.fullstack.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = MultiAgentMcpAssignProperties.MCP_ASSIGN_PROPERTIES_PREFIX, name = "enabled", havingValue = "true")
@Slf4j
public class McpFactory {

    /**
     * MCP代理json map
     * key: agentName value: MCP代理json
     */
    private final Map<String, MultiAgentMcpAssignProperties.McpServerConfig> mcpAgentConfigs;

    /**
     * MCP异步客户端配置器
     */
    private final McpAsyncClientConfigurer mcpAsyncClientConfigurer;

    /**
     * MCP客户端通用属性
     */
    private final McpClientCommonProperties commonProperties;

    /**
     * WebClient构建器模板
     */
    private final WebClient.Builder webClientBuilderTemplate;

    /**
     * JSON对象映射器
     */
    private final ObjectMapper objectMapper;

    public McpFactory(@Qualifier("agent2mcpConfig") Map<String, MultiAgentMcpAssignProperties.McpServerConfig> mcpAgentConfigs,
                      McpAsyncClientConfigurer mcpAsyncClientConfigurer,
                      McpClientCommonProperties commonProperties,
                      WebClient.Builder webClientBuilderTemplate,
                      ObjectMapper objectMapper) {
        this.mcpAgentConfigs = mcpAgentConfigs;
        this.mcpAsyncClientConfigurer = mcpAsyncClientConfigurer;
        this.commonProperties = commonProperties;
        this.webClientBuilderTemplate = webClientBuilderTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建MCP客户端
     *
     * @param agentName 智能体名称
     * @return AsyncMcpToolCallbackProvider
     */
    public AsyncMcpToolCallbackProvider createMcpClient(String agentName) {
        if (mcpAgentConfigs == null || mcpAsyncClientConfigurer == null) {
            log.warn("mcp agent configs or mcp async client configurer is null, agentName:{}", agentName);
            return null;
        }
        MultiAgentMcpAssignProperties.McpServerConfig mcpServerConfig = mcpAgentConfigs.get(agentName);
        if (mcpServerConfig == null) {
            log.warn("No mcpServerConfig for {}", agentName);
            return null;
        }
        List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();
        try {
            for (MultiAgentMcpAssignProperties.McpServerInfo mcpServerInfo : mcpServerConfig.mcpServers()) {
                if (Boolean.FALSE.equals(mcpServerInfo.enabled())) {
                    log.info("mcp server {} is disabled in createMcpClient, agentName:{}", mcpServerInfo.url() + mcpServerInfo.sseEndpoint(), agentName);
                    continue;
                }
                List<NamedClientMcpTransport> namedClientMcpTransportList = createMcpTransports(agentName, mcpServerInfo);
                for (NamedClientMcpTransport namedTransport : namedClientMcpTransportList) {
                    McpSchema.Implementation clientInfo = new McpSchema.Implementation(commonProperties.getName(),
                            commonProperties.getVersion());
                    McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport())
                            .clientInfo(clientInfo);
                    spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
                    McpAsyncClient client = spec.build();
                    // 初始化MCP客户端
                    client.initialize().block(java.time.Duration.ofMinutes(2));
                    mcpAsyncClients.add(client);
                    log.info("create mcp client, agentName:{}, url:{}", agentName, mcpServerInfo.url() + mcpServerInfo.sseEndpoint());
                }
            }
            if (!CollectionUtils.isEmpty(mcpAsyncClients)) {
                log.info("create mcp client success, agentName:{}, mcpAsyncClients size:{}", agentName, mcpAsyncClients.size());
                return new AsyncMcpToolCallbackProvider(mcpAsyncClients);
            }
        } catch (Exception e) {
            log.error("create mcp client error, agentName:{}", agentName, e);
        }
        return null;
    }

    /**
     * 为MCP server创建NamedClientMcpTransport
     *
     * @param agentName     智能体名称
     * @param mcpServerInfo MCP Server json
     * @return List<NamedClientMcpTransport>
     */
    private List<NamedClientMcpTransport> createMcpTransports(String agentName, MultiAgentMcpAssignProperties.McpServerInfo mcpServerInfo) {
        List<NamedClientMcpTransport> transports = new ArrayList<>();
        if (!mcpServerInfo.enabled()) {
            log.info("mcp server {} is disabled in createMcpTransports, agentName:{}", mcpServerInfo.url() + mcpServerInfo.sseEndpoint(), agentName);
            return new ArrayList<>();
        }
        WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(mcpServerInfo.url());
        String sseEndpoint = mcpServerInfo.sseEndpoint() != null ? mcpServerInfo.sseEndpoint() : "/sse";
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(mcpServerInfo.url())
//                .builder(webClientBuilder)
                .sseEndpoint(sseEndpoint)
                .objectMapper(objectMapper)
                .build();
        String transportName = agentName + "-" + mcpServerInfo.url().hashCode();
        transports.add(new NamedClientMcpTransport(transportName, transport));
        return transports;
    }


}
