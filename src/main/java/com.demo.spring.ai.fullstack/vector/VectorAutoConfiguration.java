package com.demo.spring.ai.fullstack.vector;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * VectorAutoConfiguration
 *
 * @author benym
 * @date 2025/8/1 17:57
 */
@EnableConfigurationProperties({VectorProperties.class})
@Configuration(enforceUniqueMethods = false)
@Slf4j
public class VectorAutoConfiguration {

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:test_vector_store}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity:cosine}")
    private SimilarityFunction similarityFunction;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1536}")
    private int dimensions;

    @Bean(name = "vectorStore")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "demo.ai.vector.vector-type", havingValue = "elasticsearch", matchIfMissing = true)
    public AbstractObservationVectorStore vectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        log.info("create elasticsearch vector store");
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        // Optional: defaults to "spring-ai-document-index"
        options.setIndexName(indexName);
        // Optional: defaults to COSINE
        options.setSimilarity(similarityFunction);
        // Optional: defaults to model dimensions or 1536
        options.setDimensions(dimensions);
        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                // Optional: use custom options
                .options(options)
                // Optional: defaults to false
                .initializeSchema(true)
                // Optional: defaults to TokenCountBatchingStrategy
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    @Bean(name = "vectorStore")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "demo.ai.vector.vector-type", havingValue = "elasticsearch", matchIfMissing = true)
    public AbstractObservationVectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("create simle vector store");
        return SimpleVectorStore.builder(embeddingModel)
                // Optional: defaults to TokenCountBatchingStrategy
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }
}
