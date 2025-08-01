package com.demo.spring.ai.fullstack.advisor.post;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import java.util.Collections;
import java.util.List;

/**
 * 只保留第一个文档
 *
 * @author benym
 */
public class DocumentSelectProcess implements DocumentPostProcessor {

    @NotNull
    @Override
    public List<Document> process(@NotNull Query query, @NotNull List<Document> documents) {
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(documents.get(0));
    }
}
