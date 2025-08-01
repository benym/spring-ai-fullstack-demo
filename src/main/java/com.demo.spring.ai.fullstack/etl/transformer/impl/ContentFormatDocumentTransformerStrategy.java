package com.demo.spring.ai.fullstack.etl.transformer.impl;

import com.demo.spring.ai.fullstack.domain.enums.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentFormatDocumentTransformerStrategy implements DocumentTransformerStrategy {
    @Override
    public List<Document> transform(List<Document> documents) {
        DefaultContentFormatter defaultContentFormatter = DefaultContentFormatter.defaultConfig();
        ContentFormatTransformer contentFormatTransformer = new ContentFormatTransformer(defaultContentFormatter);
        return contentFormatTransformer.apply(documents);
    }

    @Override
    public String transformType() {
        return TransformerTypeEnum.CONTENT_FORMAT.getCode();
    }
}
