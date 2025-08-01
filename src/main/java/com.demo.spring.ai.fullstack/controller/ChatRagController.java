package com.demo.spring.ai.fullstack.controller;

import com.demo.spring.ai.fullstack.advisor.post.DocumentSelectProcess;
import com.demo.spring.ai.fullstack.advisor.pre.StreamMultiQueryExpander;
import com.demo.spring.ai.fullstack.advisor.pre.StreamRewriteQueryTransformer;
import com.demo.spring.ai.fullstack.advisor.pre.StreamTranslationQueryTransformer;
import com.demo.spring.ai.fullstack.controller.streamProcess.ChatStreamProcessor;
import com.demo.spring.ai.fullstack.common.ChatEvent;
import com.demo.spring.ai.fullstack.mcp.McpFactory;
import com.demo.spring.ai.fullstack.mcp.McpForAgentSelector;
import com.demo.spring.ai.fullstack.prompt.PromptLoader;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * chat rag
 */
@RestController
@Slf4j
public class ChatRagController {

//    private final ChatClient.Builder chatClientBuilder;

    private final ChatClient.Builder thinkModelChatClientBuilder;

    private final ChatClient.Builder noThinkModelChatClientBuilder;

    private final VectorStore vectorStore;

    private final PromptLoader promptLoader;

    private final McpFactory mcpFactory;

    public ChatRagController(ChatClient.Builder thinkModelChatClientBuilder,
                             ChatClient.Builder noThinkModelChatClientBuilder,
                             @Qualifier("vectorStore") VectorStore vectorStore,
                             @Qualifier("innerMcpToolCallBackProvider") ToolCallbackProvider toolCallbackProvider,
                             PromptLoader promptLoader,
                             @Qualifier("messageMemory") MessageWindowChatMemory messageWindowChatMemory,
                             McpForAgentSelector mcpForAgentSelector,
                             McpFactory mcpFactory) {
        this.promptLoader = promptLoader;
        this.mcpFactory = mcpFactory;
//        ToolCallback[] weatherAgentTools = mcpForAgentSelector.getMcpToolCallbacks("weatherAgent");
//        ToolCallback[] searchAgentTools = mcpForAgentSelector.getMcpToolCallbacks("searchAgent");
//        this.chatClientBuilder = builder
//                // (可选单点注入)注册@Tool应用内的MCP
//                .defaultTools(timeService)
//                .defaultToolCallbacks(toolCallbackProvider)
//                .defaultAdvisors(
//                        // 日志Advisor
//                        new SimpleLoggerAdvisor(),
//                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build()
//                );
        this.thinkModelChatClientBuilder = thinkModelChatClientBuilder
                // (可选单点注入)注册@Tool应用内的MCP
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(
                        // 日志Advisor
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build()
                );
        this.noThinkModelChatClientBuilder = noThinkModelChatClientBuilder
                // (可选单点注入)注册@Tool应用内的MCP
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(
                        // 日志Advisor
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build()
                );
        this.vectorStore = vectorStore;
    }

    /**
     * 流式响应chat接口，适用于RetrievalAugmentationAdvisor内各个组件都采用流式返回消息
     *
     * @param query       用户输入
     * @param userId      用户id
     * @param sessionId   会话id
     * @param deepThink   是否开启深度思考
     * @param reActPrompt 是否使用ReAct提示
     * @param cotPrompt   是否使用CoT提示
     * @param response    返回响应
     * @return Flux<ChatEvent>
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEvent> streamChat(@RequestParam(value = "query") String query,
                                      @RequestParam(value = "userId") String userId,
                                      @RequestParam(value = "sessionId") String sessionId,
                                      @RequestParam(value = "deepThink") Boolean deepThink,
                                      @RequestParam(value = "reActPrompt") Boolean reActPrompt,
                                      @RequestParam(value = "cotPrompt") Boolean cotPrompt,
                                      HttpServletResponse response) {
        log.info("query: {}, userId: {}, sessionId: {}, deepThink: {}, reActPrompt: {}, cotPrompt: {}",
                query, userId, sessionId, deepThink, reActPrompt, cotPrompt);
        response.setCharacterEncoding("UTF-8");
        ChatClient.Builder chatClientBuilder = deepThink ? this.thinkModelChatClientBuilder : this.noThinkModelChatClientBuilder;
        // 1. 查询改写
        StreamRewriteQueryTransformer queryTransformer = StreamRewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 2. 多查询扩展
        StreamMultiQueryExpander multiQueryExpander = StreamMultiQueryExpander.builder()
                .numberOfQueries(3)
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 3. 查询压缩(结合历史聊天)
        CompressionQueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 4. 语言转化
        StreamTranslationQueryTransformer translationQueryTransformer = StreamTranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("Chinese")
                .build();
        // 5. Retrieval检索增强
        // 5.1 VectorStoreDocumentRetriever
        VectorStoreDocumentRetriever vectorStoreDocumentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        // 5.2 检索后的文档拼接去重
        ConcatenationDocumentJoiner concatenationDocumentJoiner = new ConcatenationDocumentJoiner();
        // (可选自定义) 后置检索 Post-Retrieval
        // 6. 检索生成
        // 6.1 将检索到的文档拼接到用户问题中，允许空内容检索，false时检索不到就不会回答
        ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
        // 构建增强Advisor
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
//                .queryExpander(multiQueryExpander)
//                    .queryTransformers(queryTransformer, translationQueryTransformer, compressionQueryTransformer)
//                .queryTransformers(queryTransformer, translationQueryTransformer)
                .documentRetriever(vectorStoreDocumentRetriever)
                .documentJoiner(concatenationDocumentJoiner)
//                .documentPostProcessors(new DocumentSelectProcess())
                .queryAugmenter(contextualQueryAugmenter)
                .build();
        List<Message> messages = new ArrayList<>();
        if (Boolean.TRUE.equals(reActPrompt)) {
            String reActPromptTxt = promptLoader.loadPrompt("/react/ReAct.txt");
            log.info("reActPrompt: {}", reActPromptTxt);
            messages.add(new SystemMessage(reActPromptTxt));
        }
        if (Boolean.TRUE.equals(cotPrompt)) {
            String cotPromptTxt = promptLoader.loadPrompt("/cot/cot.txt");
            log.info("cotPrompt: {}", cotPromptTxt);
            messages.add(new SystemMessage(cotPromptTxt));
        }
        messages.add(new UserMessage(query));
        Prompt prompt = Prompt.builder()
                .messages(messages)
                .build();
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClientBuilder.build()
                .prompt(prompt);
        if (Boolean.TRUE.equals(deepThink)) {
            AsyncMcpToolCallbackProvider searchAgentToolCallbackProvider = mcpFactory != null ? mcpFactory.createMcpClient("searchAgent")
                    : null;
            if (searchAgentToolCallbackProvider != null) {
                // 不能用defaultToolBack会注入到bean中，下一轮对话会报错重复Tool
                chatClientRequestSpec.toolCallbacks(searchAgentToolCallbackProvider.getToolCallbacks());
            }
        } else {
            AsyncMcpToolCallbackProvider weatherAgentToolCallbackProvider = mcpFactory != null ? mcpFactory.createMcpClient("weatherAgent")
                    : null;
            if (weatherAgentToolCallbackProvider != null) {
                // 不能用defaultToolBack会注入到bean中，下一轮对话会报错重复Tool
                chatClientRequestSpec.toolCallbacks(weatherAgentToolCallbackProvider.getToolCallbacks());
            }
        }
        Flux<ChatResponse> chatResponseFlux = chatClientRequestSpec
                .advisors(retrievalAugmentationAdvisor)
                .advisors(ad -> ad.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse();
        return ChatStreamProcessor.processStream(chatResponseFlux, deepThink);
    }

    /**
     * 同步chat接口，仅适用于RetrievalAugmentationAdvisor内各个组件都采用call()方法阻塞式返回消息
     *
     * @param query     用户输入
     * @param userId    用户id
     * @param sessionId 会话id
     * @param deepThink 是否开启深度思考
     * @param response  返回响应
     * @return String
     */
    @GetMapping(value = "/chat/sync", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String syncChat(@RequestParam(value = "query") String query,
                           @RequestParam(value = "userId") String userId,
                           @RequestParam(value = "sessionId") String sessionId,
                           @RequestParam(value = "deepThink") Boolean deepThink,
                           HttpServletResponse response) {
        log.info("query: {}, userId: {}, sessionId: {}, deepThink: {}", query, userId, sessionId, deepThink);
        response.setCharacterEncoding("UTF-8");
        ChatClient.Builder chatClientBuilder = deepThink ? this.thinkModelChatClientBuilder : this.noThinkModelChatClientBuilder;
        // 1. 查询改写
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 2. 多查询扩展
        MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
                .numberOfQueries(3)
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 3. 查询压缩(结合历史聊天)
        CompressionQueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        // 4. 语言转化
        TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("Chinese")
                .build();
        // 5. Retrieval检索增强
        // 5.1 VectorStoreDocumentRetriever
        VectorStoreDocumentRetriever vectorStoreDocumentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        // 5.2 检索后的文档拼接去重
        ConcatenationDocumentJoiner concatenationDocumentJoiner = new ConcatenationDocumentJoiner();
        // (可选自定义) 后置检索 Post-Retrieval
        // 6. 检索生成
        // 6.1 将检索到的文档拼接到用户问题中，允许空内容检索，false时检索不到就不会回答
        ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
        // 构建增强Advisor
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryExpander(multiQueryExpander)
                .queryTransformers(queryTransformer, translationQueryTransformer, compressionQueryTransformer)
                .documentRetriever(vectorStoreDocumentRetriever)
                .documentJoiner(concatenationDocumentJoiner)
                .documentPostProcessors(new DocumentSelectProcess())
                .queryAugmenter(contextualQueryAugmenter)
                .build();
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(query));
        Prompt prompt = Prompt.builder()
                .messages(messages)
                .build();
        return chatClientBuilder.build()
                .prompt(prompt)
                .advisors(retrievalAugmentationAdvisor)
                .advisors(ad -> ad.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }
}
