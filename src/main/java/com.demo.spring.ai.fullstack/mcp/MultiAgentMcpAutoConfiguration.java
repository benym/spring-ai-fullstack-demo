package com.demo.spring.ai.fullstack.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多agent mcp分配实现
 */
@ConditionalOnProperty(prefix = MultiAgentMcpAssignProperties.MCP_ASSIGN_PROPERTIES_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties({MultiAgentMcpAssignProperties.class, McpClientCommonProperties.class})
@Configuration
@Slf4j
public class MultiAgentMcpAutoConfiguration {

    @Resource
    private MultiAgentMcpAssignProperties multiAgentMcpAssignProperties;

    @Resource
    private McpClientCommonProperties commonProperties;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private WebClient.Builder webClientBuilderTemplate;

    /**
     * 读取项目中的mcp JSON配置文件
     * key: agent名称，value: mcp配置实体
     */
    @Bean(name = "agent2mcpConfig")
    public Map<String, MultiAgentMcpAssignProperties.McpServerConfig> agent2mcpConfig() {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource(multiAgentMcpAssignProperties.getMcpConfigLocation());
            if (!resource.exists()) {
                return new HashMap<>();
            }
            try (InputStream inputStream = resource.getInputStream()) {
                TypeReference<Map<String, MultiAgentMcpAssignProperties.McpServerConfig>> typeRef = new TypeReference<>() {
                };
                return objectMapper.readValue(inputStream, typeRef);
            }
        } catch (IOException e) {
            log.error("读取MCP配置失败", e);
            return new HashMap<>();
        }
    }

//    /**
//     * agent对应的的MCP传输列表
//     * key：agent名称，value: mcp transport list
//     */
//    @Bean(name = "agent2Transports")
//    public Map<String, List<NamedClientMcpTransport>> agent2Transports(
//            @Qualifier("agent2mcpConfig") Map<String, MultiAgentMcpAssignProperties.McpServerConfig> mcpAgentConfigs) {
//        Map<String, List<NamedClientMcpTransport>> agent2Transports = new HashMap<>();
//
//        for (Map.Entry<String, MultiAgentMcpAssignProperties.McpServerConfig> entry : mcpAgentConfigs.entrySet()) {
//            String agentName = entry.getKey();
//            MultiAgentMcpAssignProperties.McpServerConfig config = entry.getValue();
//
//            List<NamedClientMcpTransport> transports = new ArrayList<>();
//            for (MultiAgentMcpAssignProperties.McpServerInfo serverInfo : config.mcpServers()) {
//                if (!serverInfo.enabled()) {
//                    continue;
//                }
//
////                WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(serverInfo.url());
////                String sseEndpoint = serverInfo.sseEndpoint() != null ? serverInfo.sseEndpoint() : "/sse";
////                WebFluxSseClientTransport transport = WebFluxSseClientTransport.builder(webClientBuilder)
////                        .sseEndpoint(sseEndpoint)
////                        .objectMapper(objectMapper)
////                        .build();
//                String sseEndpoint = serverInfo.sseEndpoint() != null ? serverInfo.sseEndpoint() : "/sse";
//                McpClientTransport transport = HttpClientSseClientTransport.builder(serverInfo.url())
//                        .sseEndpoint(sseEndpoint)
//                        .objectMapper(objectMapper)
//                        .build();
//                String transportName = agentName + "-" + serverInfo.url().hashCode();
//                transports.add(new NamedClientMcpTransport(transportName, transport));
//            }
//            agent2Transports.put(agentName, transports);
//        }
//        return agent2Transports;
//    }
//
//    /**
//     * 创建按代理分组的AsyncMcpToolCallbackProvider Map
//     */
//    @Bean(name = "agent2AsyncMcpToolCallbackProvider")
//    @ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = {"type"}, havingValue = "ASYNC")
//    public Map<String, AsyncMcpToolCallbackProvider> agent2AsyncMcpToolCallbackProvider(
//            @Qualifier("agent2Transports") Map<String, List<NamedClientMcpTransport>> agent2Transports,
//            ObjectProvider<McpAsyncClientConfigurer> mcpAsyncClientConfigurerProvider) {
//        Map<String, AsyncMcpToolCallbackProvider> providerMap = new HashMap<>();
//        McpAsyncClientConfigurer mcpAsyncClientConfigurer = mcpAsyncClientConfigurerProvider.getObject();
//
//        agent2Transports.forEach((agentName, transports) -> {
//            List<McpAsyncClient> mcpAsyncClients = new ArrayList<>();
//
//            for (NamedClientMcpTransport namedTransport : transports) {
//                McpSchema.Implementation clientInfo = new McpSchema.Implementation(
//                        this.connectedClientName(commonProperties.getName(), namedTransport.name()),
//                        commonProperties.getVersion());
//                McpClient.AsyncSpec spec = McpClient.async(namedTransport.transport())
//                        .clientInfo(clientInfo)
//                        .requestTimeout(commonProperties.getRequestTimeout());
//                spec = mcpAsyncClientConfigurer.configure(namedTransport.name(), spec);
//                McpAsyncClient client = spec.build();
//                if (commonProperties.isInitialized()) {
//                    client.initialize().block(Duration.ofMinutes(2));
//                }
//
//                mcpAsyncClients.add(client);
//            }
//            providerMap.put(agentName, new AsyncMcpToolCallbackProvider(mcpAsyncClients));
//        });
//        return providerMap;
//    }
//
//    /**
//     * 创建按代理分组的SyncMcpToolCallbackProvider Map
//     */
//    @Bean(name = "agent2SyncMcpToolCallbackProvider")
//    @ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = {"type"}, havingValue = "SYNC")
//    public Map<String, SyncMcpToolCallbackProvider> agent2SyncMcpToolCallbackProvider(
//            @Qualifier("agent2Transports") Map<String, List<NamedClientMcpTransport>> agent2Transports,
//            ObjectProvider<McpSyncClientConfigurer> mcpSyncClientConfigurerProvider) {
//        Map<String, SyncMcpToolCallbackProvider> providerMap = new HashMap<>();
//        McpSyncClientConfigurer mcpSyncClientConfigurer = mcpSyncClientConfigurerProvider.getObject();
//
//        agent2Transports.forEach((agentName, transports) -> {
//            List<McpSyncClient> mcpSyncClients = new ArrayList<>();
//
//            for (NamedClientMcpTransport namedTransport : transports) {
//                McpSchema.Implementation clientInfo = new McpSchema.Implementation(
//                        this.connectedClientName(commonProperties.getName(), namedTransport.name()),
//                        commonProperties.getVersion());
//                McpClient.SyncSpec spec = McpClient.sync(namedTransport.transport())
//                        .clientInfo(clientInfo)
//                        .requestTimeout(commonProperties.getRequestTimeout());
//                spec = mcpSyncClientConfigurer.configure(namedTransport.name(), spec);
//                McpSyncClient client = spec.build();
//                if (commonProperties.isInitialized()) {
//                    client.initialize();
//                }
//
//                mcpSyncClients.add(client);
//            }
//            providerMap.put(agentName, new SyncMcpToolCallbackProvider(mcpSyncClients));
//        });
//        return providerMap;
//    }
//
//    private String connectedClientName(String clientName, String serverConnectionName) {
//        return clientName + "-" + serverConnectionName;
//    }
}
