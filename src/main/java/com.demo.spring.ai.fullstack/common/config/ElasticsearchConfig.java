package com.demo.spring.ai.fullstack.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String url;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity}")
    private SimilarityFunction similarityFunction;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;


    @Bean
    public RestClient restClient() {
        // 解析URL
        String[] uriParts = url.split(",");
        HttpHost[] hosts = new HttpHost[uriParts.length];
        for (int i = 0; i < uriParts.length; i++) {
            String[] hostPortParts = uriParts[i].trim().split(":");
            String host = hostPortParts[0];
            int port = Integer.parseInt(hostPortParts[1]);
            hosts[i] = new HttpHost(host, port);
        }
        // 创建凭证提供者
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        log.info("create elasticsearch rest client");
        // 构建RestClient
        return RestClient.builder(hosts)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    return httpClientBuilder;
                })
                .build();
    }

    @Bean
    @Qualifier("elasticsearchVectorStore")
    public ElasticsearchVectorStore vectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
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
}
