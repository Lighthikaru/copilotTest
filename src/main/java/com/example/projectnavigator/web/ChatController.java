package com.example.projectnavigator.web;

import com.example.projectnavigator.dto.ChatRequest;
import com.example.projectnavigator.service.ChatService;
import jakarta.validation.Valid;
import java.util.concurrent.Executor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final Executor applicationTaskExecutor;

    public ChatController(ChatService chatService, Executor applicationTaskExecutor) {
        this.chatService = chatService;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        applicationTaskExecutor.execute(() -> chatService.stream(request, emitter));
        return emitter;
    }
}
