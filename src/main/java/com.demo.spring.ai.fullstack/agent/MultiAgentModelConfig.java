package com.demo.spring.ai.fullstack.agent;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 多Agent配置
 */
@Configuration
public class MultiAgentModelConfig implements InitializingBean {

    private static final String BEAN_NAME_SUFFIX = "ChatClientBuilder";

    private final List<ModelParamRepositoryImpl.AgentModel> models;

    private final DashScopeConnectionProperties commonProperties;

    private final ToolCallingManager toolCallingManager;

    private final BiConsumer<String, DashScopeChatModel> registerConsumer;

    public MultiAgentModelConfig(ConfigurableBeanFactory beanFactory,
                                 List<ModelParamRepositoryImpl.AgentModel> models,
                                 DashScopeConnectionProperties commonProperties,
                                 ToolCallingManager toolCallingManager,
                                 ModelParamRepository modelParamRepository) {
        this.models = modelParamRepository.loadModels();
        this.commonProperties = commonProperties;
        this.toolCallingManager = toolCallingManager;
        // 注入ChatClient，key: 配置的模型名称+ChatClientBuilder，value: ChatClient.Builder
        this.registerConsumer = (key, value) -> beanFactory.registerSingleton(key.concat(BEAN_NAME_SUFFIX),
                ChatClient.create(value).mutate());
    }

    /**
     * 组装所有模型
     *
     * @return key:模型名称 value:模型
     */
    private Map<String, DashScopeChatModel> agentModels() {
        return models.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ModelParamRepositoryImpl.AgentModel::name,
                        model -> DashScopeChatModel.builder()
                                .dashScopeApi(DashScopeApi.builder()
                                        .apiKey(commonProperties.getApiKey())
                                        .build())
                                .toolCallingManager(toolCallingManager)
                                .defaultOptions(DashScopeChatOptions.builder()
                                        .withModel(model.modelName())
                                        .withTemperature(DashScopeChatModel.DEFAULT_TEMPERATURE)
                                        // 是否开启深度思考
                                        .withEnableThinking(model.enableThink())
                                        .build())
                                .build(),
                        (existing, replacement) -> existing)
                );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.agentModels().forEach(registerConsumer);
    }
}
