package com.demo.spring.ai.fullstack.etl.transformer.impl;

import com.demo.spring.ai.fullstack.domain.enums.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MetaDataDocumentTransformerStrategy implements DocumentTransformerStrategy {

    @Override
    public List<Document> transform(List<Document> documents) {
        return List.of();
    }

    @Override
    public String transformType() {
        return TransformerTypeEnum.META_DATA.getCode();
    }

    @Override
    public List<Document> transformWithMeta(List<Document> documents, Map<String, String> metaDataMap) {
        for (Document doc : documents) {
            doc.getMetadata().putAll(metaDataMap);
        }
        return documents;
    }
}
