package com.demo.spring.ai.fullstack.etl.read;

import com.demo.spring.ai.fullstack.domain.enums.FileTypeEnum;
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
public class ReaderFactory implements InitializingBean {
    @Resource
    private ApplicationContext applicationContext;

    private Map<String, DocumentReaderStrategy> strategyMap = new HashMap<>(8);

    public DocumentReaderStrategy getStrategy(String fileType) {
        if (strategyMap.get(fileType) == null) {
            return strategyMap.get(FileTypeEnum.DEFAULT.getCode());
        }
        return strategyMap.get(fileType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, DocumentReaderStrategy> beansOfType = applicationContext.getBeansOfType(DocumentReaderStrategy.class);
        strategyMap = Optional.of(beansOfType)
                .map(beansOfTypeMap -> beansOfTypeMap.values().stream()
                        .filter(documentReaderStrategy -> documentReaderStrategy.fileType() != null)
                        .collect(Collectors.toMap(DocumentReaderStrategy::fileType, Function.identity())))
                .orElse(new HashMap<>(8));
    }
}
