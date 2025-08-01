package com.demo.spring.ai.fullstack.etl.transformer.impl;

import com.demo.spring.ai.fullstack.common.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordDocumentTransformerStrategy implements DocumentTransformerStrategy {

    private final ChatModel chatModel;

    public KeywordDocumentTransformerStrategy(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public List<Document> transform(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(this.chatModel, 3);
        return keywordMetadataEnricher.apply(documents);
    }

    @Override
    public String transformType() {
        return TransformerTypeEnum.KEYWORD.getCode();
    }
}
