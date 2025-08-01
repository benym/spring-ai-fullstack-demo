package com.demo.spring.ai.fullstack.agent;

import java.util.List;


public interface ModelParamRepository {

    List<ModelParamRepositoryImpl.AgentModel> loadModels();
}
