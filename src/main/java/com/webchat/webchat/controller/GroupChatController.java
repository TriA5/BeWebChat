package com.webchat.webchat.controller;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.dto.GroupConversationDTO;
import com.webchat.webchat.dto.GroupMemberDTO;
import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.service.chat.GroupChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupChatController {
    private final GroupChatService groupChatService;

    @PostMapping("/create")
    public ResponseEntity<GroupConversation> createGroup(
            @RequestParam UUID creatorId,
            @RequestParam String groupName,
            @RequestBody List<UUID> initialMemberIds) {
        return ResponseEntity.ok(groupChatService.createGroup(creatorId, groupName, initialMemberIds));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Void> joinGroup(@PathVariable UUID groupId, @RequestParam UUID userId) {
        groupChatService.joinGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable UUID groupId,
            @RequestParam UUID senderId,
            @RequestBody String content) {
        return ResponseEntity.ok(groupChatService.sendGroupMessage(groupId, senderId, content));
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupChatService.getGroupMessages(groupId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupConversationDTO>> getGroupsForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(groupChatService.getGroupsForUser(userId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDTO>> getGroupMembers(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupChatService.getGroupMembers(groupId));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            @RequestParam UUID requesterId) {
        groupChatService.removeMember(groupId, userId, requesterId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID groupId,
            @RequestParam UUID requesterId) {
        groupChatService.deleteGroup(groupId, requesterId);
        return ResponseEntity.ok().build();
    }
}