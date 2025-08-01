package com.demo.spring.ai.fullstack.common;

import lombok.Data;

@Data
public class ChatEvent {

    public ChatEvent(EventType eventType, String content, boolean isThinking) {
        this.type = eventType;
        this.content = content;
        this.isThinking = isThinking;
    }

    public enum EventType {
        // 思考过程
        THINKING,
        // 最终答案
        ANSWER
    }

    private EventType type;

    private String content;

    // 标记当前内容是否为思考过程
    private boolean isThinking;
}
