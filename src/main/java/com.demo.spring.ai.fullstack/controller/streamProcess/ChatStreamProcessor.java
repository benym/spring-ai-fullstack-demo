package com.demo.spring.ai.fullstack.controller.streamProcess;

import com.demo.spring.ai.fullstack.common.ChatEvent;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Flux-SSE流 深度思考融合处理
 */
@Slf4j
public class ChatStreamProcessor {

    /**
     * 处理chat模式Flux流
     *
     * @param chatResponseFlux 模型回答Flux流
     * @param deepThink        是否开启深度思考
     * @return Flux<ChatEvent>
     */
    public static Flux<ChatEvent> processStream(Flux<ChatResponse> chatResponseFlux, Boolean deepThink) {
        return chatResponseFlux
                .flatMapIterable(response -> processGenerations(response, deepThink))
                .onErrorResume(ChatStreamProcessor::handleError);
    }

    private static List<ChatEvent> processGenerations(ChatResponse response, Boolean deepThink) {
        return response.getResults().stream()
                .flatMap(generation -> buildEvents(generation, deepThink).stream())
                .collect(Collectors.toList());
    }

    private static List<ChatEvent> buildEvents(Generation generation, Boolean deepThink) {
        List<ChatEvent> events = new ArrayList<>();
        AssistantMessage msg = generation.getOutput();
        Map<String, Object> metadata = msg.getMetadata();
        // 深度思考处理
        if (Boolean.TRUE.equals(deepThink)) {
            Optional.ofNullable(metadata.get("reasoningContent"))
                    .map(Object::toString)
                    .filter(content -> !content.isEmpty())
                    .ifPresent(reasoning ->
                            events.add(new ChatEvent(ChatEvent.EventType.THINKING, reasoning, true)));
        }
        // 最终答案处理
        Optional.of(msg.getText())
                .filter(text -> !text.isEmpty())
                .ifPresent(answer ->
                        events.add(new ChatEvent(ChatEvent.EventType.ANSWER, answer, false)));

        return events;
    }

    private static Publisher<? extends ChatEvent> handleError(Throwable e) {
        if (e instanceof TimeoutException) {
            log.error("流处理超时", e);
            return Flux.just(new ChatEvent(ChatEvent.EventType.ANSWER, "处理超时，请稍后再试", false));
        }
        log.error("流处理错误", e);
        return Flux.just(new ChatEvent(ChatEvent.EventType.ANSWER, "处理失败: " + e.getMessage(), false));
    }
}
