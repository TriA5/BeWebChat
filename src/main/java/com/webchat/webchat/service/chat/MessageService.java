package com.webchat.webchat.service.chat;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.entity.Conversation;
import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.entity.Message;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.ConversationRepository;
import com.webchat.webchat.repository.GroupConversationRepository;
import com.webchat.webchat.repository.GroupMemberRepository;
import com.webchat.webchat.repository.MessageRepository;
import com.webchat.webchat.repository.UserRepository;
import com.webchat.webchat.service.UploadImage.UploadImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service để xử lý gửi tin nhắn (text, image, file)
 */
@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final GroupConversationRepository groupConversationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UploadImageService uploadImageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn text trong conversation
     */
    public ChatMessageDTO sendTextMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Message message = createMessage(conversation, null, sender, content, "TEXT", null);
        Message saved = messageRepository.save(message);

        ChatMessageDTO dto = buildMessageDTO(saved, conversationId, null);
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
        return dto;
    }

    /**
     * Gửi tin nhắn image trong conversation
     */
    public ChatMessageDTO sendImageMessage(UUID conversationId, UUID senderId, MultipartFile imageFile) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            // Upload image to Cloudinary
            String imageUrl = uploadImageService.uploadImage(imageFile, "chat_" + UUID.randomUUID());

            Message message = createMessage(conversation, null, sender, "", "IMAGE", imageUrl);
            Message saved = messageRepository.save(message);

            ChatMessageDTO dto = buildMessageDTO(saved, conversationId, null);
            messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send image message: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi tin nhắn file trong conversation (có thể mở rộng sau)
     */
    public ChatMessageDTO sendFileMessage(UUID conversationId, UUID senderId, MultipartFile file) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            // Upload file to Cloudinary (có thể dùng service khác cho file)
            String fileUrl = uploadImageService.uploadImage(file, "file_" + UUID.randomUUID());

            Message message = createMessage(conversation, null, sender, file.getOriginalFilename(), "FILE", fileUrl);
            Message saved = messageRepository.save(message);

            ChatMessageDTO dto = buildMessageDTO(saved, conversationId, null);
            messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send file message: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi tin nhắn text trong group
     */
    public ChatMessageDTO sendGroupTextMessage(UUID groupId, UUID senderId, String content) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        groupMemberRepository.findByGroupAndUser(group, sender)
                .orElseThrow(() -> new RuntimeException("Không phải thành viên của nhóm"));

        Message message = createMessage(null, group, sender, content, "TEXT", null);
        Message saved = messageRepository.save(message);

        ChatMessageDTO dto = buildMessageDTO(saved, null, groupId);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
        return dto;
    }

    /**
     * Gửi tin nhắn image trong group
     */
    public ChatMessageDTO sendGroupImageMessage(UUID groupId, UUID senderId, MultipartFile imageFile) {
        try {
            GroupConversation group = groupConversationRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            groupMemberRepository.findByGroupAndUser(group, sender)
                    .orElseThrow(() -> new RuntimeException("Không phải thành viên của nhóm"));

            // Upload image to Cloudinary
            String imageUrl = uploadImageService.uploadImage(imageFile, "group_chat_" + UUID.randomUUID());

            Message message = createMessage(null, group, sender, "", "IMAGE", imageUrl);
            Message saved = messageRepository.save(message);

            ChatMessageDTO dto = buildMessageDTO(saved, null, groupId);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send group image message: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi tin nhắn file trong group (có thể mở rộng sau)
     */
    public ChatMessageDTO sendGroupFileMessage(UUID groupId, UUID senderId, MultipartFile file) {
        try {
            GroupConversation group = groupConversationRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            groupMemberRepository.findByGroupAndUser(group, sender)
                    .orElseThrow(() -> new RuntimeException("Không phải thành viên của nhóm"));

            // Upload file to Cloudinary
            String fileUrl = uploadImageService.uploadImage(file, "group_file_" + UUID.randomUUID());

            Message message = createMessage(null, group, sender, file.getOriginalFilename(), "FILE", fileUrl);
            Message saved = messageRepository.save(message);

            ChatMessageDTO dto = buildMessageDTO(saved, null, groupId);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send group file message: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method để tạo Message entity
     */
    private Message createMessage(Conversation conversation, GroupConversation group, 
                                   User sender, String content, String messageType, String fileUrl) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setGroupConversation(group);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setImageUrl(fileUrl); // Dùng chung cho cả image và file
        return message;
    }

    /**
     * Helper method để build ChatMessageDTO
     */
    private ChatMessageDTO buildMessageDTO(Message message, UUID conversationId, UUID groupId) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(conversationId);
        dto.setGroupId(groupId);
        dto.setSenderId(message.getSender().getIdUser());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setImageUrl(message.getImageUrl());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
