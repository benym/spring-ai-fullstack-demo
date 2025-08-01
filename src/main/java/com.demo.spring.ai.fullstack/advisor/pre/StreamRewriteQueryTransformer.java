package com.demo.spring.ai.fullstack.advisor.pre;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.util.PromptAssert;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;


public class StreamRewriteQueryTransformer implements QueryTransformer {

    private static final Logger logger = LoggerFactory.getLogger(StreamRewriteQueryTransformer.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			Given a user query, rewrite it to provide better results when querying a {target}.
			Remove any irrelevant information, and ensure the query is concise and specific.

			Original query:
			{query}

			Rewritten query:
			""");

    private static final String DEFAULT_TARGET = "vector store";

    private final ChatClient chatClient;

    private final PromptTemplate promptTemplate;

    private final String targetSearchSystem;

    public StreamRewriteQueryTransformer(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate,
                                   @Nullable String targetSearchSystem) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.targetSearchSystem = targetSearchSystem != null ? targetSearchSystem : DEFAULT_TARGET;

        PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "target", "query");
    }

    @NotNull
    @Override
    public Query transform(@NotNull Query query) {
        Assert.notNull(query, "query cannot be null");
        logger.debug("Rewriting query to optimize for querying a {}.", this.targetSearchSystem);
        // 收集流中的所有响应内容并拼接成完整字符串
        List<String> chunks = this.chatClient.prompt()
                .user(user -> user.text(this.promptTemplate.getTemplate())
                        .param("target", this.targetSearchSystem)
                        .param("query", query.text()))
                .stream()
                .content()
                .collectList()
                .block();
        String rewrittenQueryText = chunks != null ?
                String.join("", chunks) : "";
        if (!StringUtils.hasText(rewrittenQueryText)) {
            logger.warn("Query rewrite result is null/empty. Returning the input query unchanged.");
            return query;
        }
        return query.mutate().text(rewrittenQueryText).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ChatClient.Builder chatClientBuilder;

        @Nullable
        private PromptTemplate promptTemplate;

        @Nullable
        private String targetSearchSystem;

        private Builder() {
        }

        public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
            this.chatClientBuilder = chatClientBuilder;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder targetSearchSystem(String targetSearchSystem) {
            this.targetSearchSystem = targetSearchSystem;
            return this;
        }

        public StreamRewriteQueryTransformer build() {
            return new StreamRewriteQueryTransformer(this.chatClientBuilder, this.promptTemplate, this.targetSearchSystem);
        }

    }
}
