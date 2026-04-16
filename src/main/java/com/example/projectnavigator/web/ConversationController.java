package com.example.projectnavigator.web;

import com.example.projectnavigator.dto.ConversationDetail;
import com.example.projectnavigator.dto.ConversationItem;
import com.example.projectnavigator.dto.CreateConversationRequest;
import com.example.projectnavigator.dto.UpdateConversationRequest;
import com.example.projectnavigator.service.ConversationService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationItem> list(@PathVariable String projectId) {
        return conversationService.list(projectId);
    }

    @PostMapping
    public ConversationDetail create(@PathVariable String projectId, @RequestBody(required = false) CreateConversationRequest request) {
        return conversationService.create(projectId, request == null ? null : request.title());
    }

    @GetMapping("/{conversationId}")
    public ConversationDetail get(@PathVariable String projectId, @PathVariable String conversationId) {
        return conversationService.get(projectId, conversationId);
    }

    @PatchMapping("/{conversationId}")
    public ConversationDetail update(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @RequestBody UpdateConversationRequest request) {
        return conversationService.update(projectId, conversationId, request.title(), request.archived());
    }

    @PostMapping("/{conversationId}/compress")
    public ConversationDetail compress(@PathVariable String projectId, @PathVariable String conversationId) {
        return conversationService.compress(projectId, conversationId);
    }

    @PostMapping("/{conversationId}/restart-session")
    public ConversationDetail restartSession(@PathVariable String projectId, @PathVariable String conversationId) {
        return conversationService.restartSession(projectId, conversationId);
    }

    @DeleteMapping("/{conversationId}/messages")
    public ConversationDetail clearMessages(@PathVariable String projectId, @PathVariable String conversationId) {
        return conversationService.clearMessages(projectId, conversationId);
    }
}
