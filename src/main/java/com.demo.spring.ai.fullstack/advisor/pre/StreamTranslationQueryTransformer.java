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


public class StreamTranslationQueryTransformer implements QueryTransformer {

    private static final Logger logger = LoggerFactory.getLogger(StreamTranslationQueryTransformer.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			Given a user query, translate it to {targetLanguage}.
			If the query is already in {targetLanguage}, return it unchanged.
			If you don't know the language of the query, return it unchanged.
			Do not add explanations nor any other text.

			Original query: {query}

			Translated query:
			""");

    private final ChatClient chatClient;

    private final PromptTemplate promptTemplate;

    private final String targetLanguage;

    public StreamTranslationQueryTransformer(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate,
                                       String targetLanguage) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
        Assert.hasText(targetLanguage, "targetLanguage cannot be null or empty");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.targetLanguage = targetLanguage;

        PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "targetLanguage", "query");
    }

    @NotNull
    @Override
    public Query transform(@NotNull Query query) {
        Assert.notNull(query, "query cannot be null");

        logger.debug("Translating query to target language: {}", this.targetLanguage);

        List<String> translatedQueryText = this.chatClient.prompt()
                .user(user -> user.text(this.promptTemplate.getTemplate())
                        .param("targetLanguage", this.targetLanguage)
                        .param("query", query.text()))
                .stream()
                .content()
                .collectList()
                .block();
        String queryText = translatedQueryText != null ?
                String.join("", translatedQueryText) : "";
        if (!StringUtils.hasText(queryText)) {
            logger.warn("Query translation result is null/empty. Returning the input query unchanged.");
            return query;
        }

        return query.mutate().text(queryText).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ChatClient.Builder chatClientBuilder;

        @Nullable
        private PromptTemplate promptTemplate;

        private String targetLanguage;

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

        public Builder targetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
            return this;
        }

        public StreamTranslationQueryTransformer build() {
            return new StreamTranslationQueryTransformer(this.chatClientBuilder, this.promptTemplate, this.targetLanguage);
        }

    }
}
