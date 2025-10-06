package com.webchat.webchat.controller;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.service.chat.ChatService;
import com.webchat.webchat.service.chat.GroupChatService;

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

    private final GroupChatService groupChatService; // Đã thêm inject GroupChatService
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
    public void onSendMessage(@Payload ChatMessageDTO payload) { // Đồng bộ với ChatMessageDTO
        chatService.sendMessage(payload.getConversationId(), payload.getSenderId(), payload.getContent());
    }

    @MessageMapping("/group.send")
    public void onSendGroupMessage(@Payload ChatMessageDTO payload) {
        groupChatService.sendGroupMessage(payload.getGroupId(), payload.getSenderId(), payload.getContent());
    }

    @PostMapping("/{conversationId}/send-image")
    public ChatMessageDTO sendImageMessage(
            @PathVariable UUID conversationId,
            @RequestParam UUID senderId,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile imageFile) {
        return chatService.sendImageMessage(conversationId, senderId, imageFile);
    }

    @PostMapping("/group/{groupId}/send-image")
    public ChatMessageDTO sendGroupImageMessage(
            @PathVariable UUID groupId,
            @RequestParam UUID senderId,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile imageFile) {
        return groupChatService.sendGroupImageMessage(groupId, senderId, imageFile);
    }

    @PostMapping("/{conversationId}/send-file")
    public ChatMessageDTO sendFileMessage(
            @PathVariable UUID conversationId,
            @RequestParam UUID senderId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return chatService.sendFileMessage(conversationId, senderId, file);
    }

    @PostMapping("/group/{groupId}/send-file")
    public ChatMessageDTO sendGroupFileMessage(
            @PathVariable UUID groupId,
            @RequestParam UUID senderId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return groupChatService.sendGroupFileMessage(groupId, senderId, file);
    }
}
