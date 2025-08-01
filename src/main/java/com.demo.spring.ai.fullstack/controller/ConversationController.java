package com.demo.spring.ai.fullstack.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话
 */
@RestController
@RequestMapping("/conversation")
@Slf4j
public class ConversationController {

    @Resource
    private MessageWindowChatMemory messageWindowChatMemory;

    @GetMapping("/messages")
    public List<Message> messages(@RequestParam(value = "sessionId") String sessionId) {
        return messageWindowChatMemory.get(sessionId);
    }
}
