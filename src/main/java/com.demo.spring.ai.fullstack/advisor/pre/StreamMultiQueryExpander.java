package com.demo.spring.ai.fullstack.advisor.pre;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.util.PromptAssert;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class StreamMultiQueryExpander implements QueryExpander {

    private static final Logger logger = LoggerFactory.getLogger(StreamMultiQueryExpander.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
            You are an expert at information retrieval and search optimization.
            Your task is to generate {number} different versions of the given query.

            Each variant must cover different perspectives or aspects of the topic,
            while maintaining the core intent of the original query. The goal is to
            expand the search space and improve the chances of finding relevant information.

            Do not explain your choices or add any other text.
            Provide the query variants separated by newlines.

            Original query: {query}

            Query variants:
            """);

    private static final Boolean DEFAULT_INCLUDE_ORIGINAL = true;

    private static final Integer DEFAULT_NUMBER_OF_QUERIES = 3;

    private final ChatClient chatClient;

    private final PromptTemplate promptTemplate;

    private final boolean includeOriginal;

    private final int numberOfQueries;

    public StreamMultiQueryExpander(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate,
                                    @Nullable Boolean includeOriginal, @Nullable Integer numberOfQueries) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.includeOriginal = includeOriginal != null ? includeOriginal : DEFAULT_INCLUDE_ORIGINAL;
        this.numberOfQueries = numberOfQueries != null ? numberOfQueries : DEFAULT_NUMBER_OF_QUERIES;

        PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "number", "query");
    }

    @NotNull
    @Override
    public List<Query> expand(@NotNull Query query) {
        Assert.notNull(query, "query cannot be null");

        logger.debug("Generating {} query variants", this.numberOfQueries);

        List<String> response = this.chatClient.prompt()
                .user(user -> user.text(this.promptTemplate.getTemplate())
                        .param("number", this.numberOfQueries)
                        .param("query", query.text()))
                .stream()
                .content()
                .collectList()
                .block();

        if (response == null) {
            logger.warn("Query expansion result is null. Returning the input query unchanged.");
            return List.of(query);
        }
        String responseText = String.join("", response);

        var queryVariants = Arrays.asList(responseText.split("\n"));

        if (CollectionUtils.isEmpty(queryVariants) || this.numberOfQueries != queryVariants.size()) {
            logger.warn(
                    "Query expansion result does not contain the requested {} variants. Returning the input query unchanged.",
                    this.numberOfQueries);
            return List.of(query);
        }

        var queries = queryVariants.stream()
                .filter(StringUtils::hasText)
                .map(queryText -> query.mutate().text(queryText).build())
                .collect(Collectors.toList());

        if (this.includeOriginal) {
            logger.debug("Including the original query in the result");
            queries.add(0, query);
        }

        return queries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ChatClient.Builder chatClientBuilder;

        private PromptTemplate promptTemplate;

        private Boolean includeOriginal;

        private Integer numberOfQueries;

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

        public Builder includeOriginal(Boolean includeOriginal) {
            this.includeOriginal = includeOriginal;
            return this;
        }

        public Builder numberOfQueries(Integer numberOfQueries) {
            this.numberOfQueries = numberOfQueries;
            return this;
        }

        public StreamMultiQueryExpander build() {
            return new StreamMultiQueryExpander(this.chatClientBuilder, this.promptTemplate, this.includeOriginal,
                    this.numberOfQueries);
        }

    }

}

