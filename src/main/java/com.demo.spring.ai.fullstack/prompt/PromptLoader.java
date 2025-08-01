package com.demo.spring.ai.fullstack.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.demo.spring.ai.fullstack.mcp.ToolRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PromptLoader {

    private static final String PROMPT_BASE_PATH = "prompts/";

    // Cache for loaded prompt content
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Load prompt template content
     *
     * @param promptPath Relative path of prompt file (relative to prompts directory)
     * @return Prompt content
     */
    public String loadPrompt(String promptPath) {
        return promptCache.computeIfAbsent(promptPath, this::loadPromptFromResource);
    }

    /**
     * Load prompt content from resource file
     *
     * @param promptPath Prompt file path
     * @return Prompt content
     */
    private String loadPromptFromResource(String promptPath) {
        try {
            String fullPath = PROMPT_BASE_PATH + promptPath;
            Resource resource = new ClassPathResource(fullPath);
            if (!resource.exists()) {
                log.warn("Prompt file not found: {}", fullPath);
                return "";
            }
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.debug("Loaded prompt from: {}", fullPath);
            return content;
        } catch (IOException e) {
            log.error("Failed to load prompt from: {}", promptPath, e);
            return "";
        }
    }

    /**
     * Render prompt template
     *
     * @param promptPath Prompt file path
     * @param variables  Variable mapping
     * @return Rendered prompt content
     */
    public String renderPrompt(String promptPath, Map<String, Object> variables) {
        String promptContent = loadPrompt(promptPath);
        PromptTemplate template = new PromptTemplate(promptContent);
        return template.render(variables != null ? variables : Map.of());
    }

    /**
     * Render prompt template
     * 专属ReAct测试
     *
     * @param promptPath      Prompt file path
     * @param variables       Variable mapping
     * @param registeredTools Registered tools
     * @return Rendered prompt content
     */
    public String renderPrompt(String promptPath, Map<String, Object> variables,
                               List<ToolRegistryService.ToolDescription> registeredTools) {
        String promptContent = loadPrompt(promptPath);
        if (!CollectionUtils.isEmpty(registeredTools)) {
            String toolDescs = registeredTools.stream()
                    .map(this::buildToolDescription)
                    .collect(Collectors.joining("\n\n"));
            String toolNames = registeredTools.stream()
                    .map(ToolRegistryService.ToolDescription::name)
                    .collect(Collectors.joining(","));
            // 将工具信息添加到变量中
            if (variables == null) {
                variables = Map.of();
            }
            // 使用可变Map以便添加新变量
            variables = new HashMap<>(variables);
            variables.put("tool_descs", toolDescs);
            variables.put("tool_names", toolNames);
        }
        PromptTemplate template = new PromptTemplate(promptContent);
        return template.render(variables != null ? variables : Map.of());
    }

    private String buildToolDescription(ToolRegistryService.ToolDescription tool) {
        try {
            // 解析inputSchema为JSON对象
            Object parameters = objectMapper.readValue(tool.inputSchema(), Object.class);
            Map<String, Object> variables = new HashMap<>();
            variables.put("name_for_model", tool.name());
            // 这里通常使用tool的中文，简单起见用tool.name()代替
            variables.put("name_for_human", tool.name());
            variables.put("description_for_model", tool.description());
            variables.put("parameters", objectMapper.writeValueAsString(parameters));
            return this.renderPrompt("/react/sourcePrompt/QwenToolDesc.txt", variables);
        } catch (Exception e) {
            throw ExceptionFactory.bizException("Failed to build tool description for: " + tool.name(), e);
        }
    }
}
