package com.demo.spring.ai.fullstack.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


@Slf4j
@Repository
public class ModelParamRepositoryImpl implements ModelParamRepository {

    private static final String MODELS_PREFIX = "models";

    private static final String MODEL_CONFIG_FILE = "/model/model-config.json";

    private final Map<String, List<AgentModel>> modelSet;

    public ModelParamRepositoryImpl() {
        try {
            Resource resource = new ClassPathResource(MODEL_CONFIG_FILE);
            if (!resource.exists()) {
                log.warn("Model Config file not found: {}", MODEL_CONFIG_FILE);
            }
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            this.modelSet = objectMapper.readValue(content, new TypeReference<>() {
            });
            log.info("Loaded model configuration: {}", modelSet);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error in parsing model configuration", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AgentModel> loadModels() {
        return modelSet.getOrDefault(MODELS_PREFIX, List.of());
    }

    public record AgentModel(String name, String modelName, Boolean enableThink) {
    }
}
