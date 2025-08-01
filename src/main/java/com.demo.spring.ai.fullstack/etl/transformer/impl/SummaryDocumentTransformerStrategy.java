package com.demo.spring.ai.fullstack.etl.transformer.impl;

import com.demo.spring.ai.fullstack.domain.enums.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummaryDocumentTransformerStrategy implements DocumentTransformerStrategy {

    private final ChatModel chatModel;

    public SummaryDocumentTransformerStrategy(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public List<Document> transform(List<Document> documents) {
        List<SummaryMetadataEnricher.SummaryType> summaryTypes = List.of(
                SummaryMetadataEnricher.SummaryType.NEXT,
                SummaryMetadataEnricher.SummaryType.CURRENT,
                SummaryMetadataEnricher.SummaryType.PREVIOUS);
        SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(this.chatModel, summaryTypes);
        return summaryMetadataEnricher.apply(documents);
    }

    @Override
    public String transformType() {
        return TransformerTypeEnum.SUMMARY.getCode();
    }
}
