package com.demo.spring.ai.fullstack.etl.transformer;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DocumentTransformerFactory implements InitializingBean {

    @Resource
    private ApplicationContext applicationContext;

    private Map<String, DocumentTransformerStrategy> strategyMap = new HashMap<>(8);

    public DocumentTransformerStrategy getStrategy(String transformType) {
        if (strategyMap.get(transformType) == null) {
            throw ExceptionFactory.bizNoStackException("ETL_TRANSFORM_TYPE_NOT_EXIST");
        }
        return strategyMap.get(transformType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, DocumentTransformerStrategy> beansOfType = applicationContext.getBeansOfType(DocumentTransformerStrategy.class);
        strategyMap = Optional.of(beansOfType)
                .map(beansOfTypeMap -> beansOfTypeMap.values().stream()
                        .filter(transformerStrategy -> transformerStrategy.transformType() != null)
                        .collect(Collectors.toMap(DocumentTransformerStrategy::transformType, Function.identity())))
                .orElse(new HashMap<>(8));
    }
}
