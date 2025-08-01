package com.demo.spring.ai.fullstack.etl.transformer.impl;

import com.demo.spring.ai.fullstack.domain.enums.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenTextDocumentTransformerStrategy implements DocumentTransformerStrategy {
    @Override
    public List<Document> transform(List<Document> documents) {
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                // 每个文本块的目标token数量
                .withChunkSize(800)
                // 每个文本块的最小字符数
                .withMinChunkSizeChars(350)
                // 丢弃小于此长度的文本块
                .withMinChunkLengthToEmbed(5)
                // 文本中生成的最大块数
                .withMaxNumChunks(10000)
                // 是否保留分隔符
                .withKeepSeparator(true)
                .build();
        return tokenTextSplitter.apply(documents);
    }

    @Override
    public String transformType() {
        return TransformerTypeEnum.TOKEN.getCode();
    }
}
