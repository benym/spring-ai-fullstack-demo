package com.demo.spring.ai.fullstack.etl.transformer;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface DocumentTransformerStrategy {

    List<Document> transform(List<Document> documents);

    String transformType();

    default List<Document> transformWithMeta(List<Document> documents, Map<String, String> metaDataMap) {
        return transform(documents);
    }
}
