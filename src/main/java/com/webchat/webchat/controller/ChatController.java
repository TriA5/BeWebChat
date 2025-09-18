package com.webchat.webchat.controller;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.service.chat.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/ensure")
    public UUID ensureConversation(@RequestParam UUID userAId, @RequestParam UUID userBId) {
        return chatService.ensureConversation(userAId, userBId).getId();
    }

    @GetMapping("/{conversationId}/messages")
    public List<ChatMessageDTO> getMessages(@PathVariable UUID conversationId) {
        return chatService.getMessages(conversationId);
    }

    @GetMapping("/conversations")
    public List<com.webchat.webchat.dto.ConversationDTO> listConversations(@RequestParam UUID userId) {
        return chatService.getConversationsForUser(userId);
    }

    @Data
    public static class SendMessagePayload {
        private UUID conversationId;
        private UUID senderId;
        private String content;
    }

    @MessageMapping("/chat.send")
    public void onSendMessage(@Payload SendMessagePayload payload) {
        chatService.sendMessage(payload.getConversationId(), payload.getSenderId(), payload.getContent());
    }
}
