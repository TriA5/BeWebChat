package com.webchat.webchat.service.chat;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.dto.ConversationDTO;
import com.webchat.webchat.entity.Conversation;
import com.webchat.webchat.entity.Message;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.ConversationRepository;
import com.webchat.webchat.repository.MessageRepository;
import com.webchat.webchat.repository.UserRepository;
import com.webchat.webchat.service.UploadImage.FileUploadService;
import com.webchat.webchat.service.UploadImage.UploadImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UploadImageService uploadImageService;
    private final FileUploadService fileUploadService;

    public Conversation ensureConversation(UUID userAId, UUID userBId) {
        try {
            User a = userRepository.findById(userAId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userAId));
            User b = userRepository.findById(userBId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userBId));
            return conversationRepository.findBetween(a, b).orElseGet(() -> {
                Conversation c = new Conversation();
                c.setParticipant1(a);
                c.setParticipant2(b);
                Conversation saved = conversationRepository.save(c);
                ConversationDTO dto = new ConversationDTO(saved.getId(), a.getIdUser(), b.getIdUser());
                messagingTemplate.convertAndSend("/topic/conversations/" + a.getIdUser(), dto);
                messagingTemplate.convertAndSend("/topic/conversations/" + b.getIdUser(), dto);
                return saved;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure conversation: " + e.getMessage(), e);
        }
    }

    public ChatMessageDTO sendMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        User sender = userRepository.findById(senderId).orElseThrow();
        Message m = new Message();
        m.setConversation(conversation);
        m.setSender(sender);
        m.setContent(content);
        m.setMessageType("TEXT");
        Message saved = messageRepository.save(m);

        ChatMessageDTO dto = new ChatMessageDTO(saved.getId(), conversationId, null, senderId, content, "TEXT", null, null, null, null, saved.getCreatedAt());
        // publish to conversation topic
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
        return dto;
    }

    public List<ChatMessageDTO> getMessages(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(m -> new ChatMessageDTO(
                    m.getId(), 
                    conversationId, 
                    null, 
                    m.getSender().getIdUser(), 
                    m.getContent(), 
                    m.getMessageType(), 
                    m.getImageUrl(),
                    m.getFileUrl(),
                    m.getFileName(),
                    m.getFileSize(),
                    m.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getConversationsForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return conversationRepository.findByParticipant1OrParticipant2(user, user)
                .stream()
                .map(c -> new ConversationDTO(c.getId(), c.getParticipant1().getIdUser(), c.getParticipant2().getIdUser()))
                .collect(Collectors.toList());
    }

    public ChatMessageDTO sendImageMessage(UUID conversationId, UUID senderId, MultipartFile imageFile) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            // Upload image to Cloudinary
            String imageUrl = uploadImageService.uploadImage(imageFile, "chat_" + UUID.randomUUID());

            // Create message
            Message m = new Message();
            m.setConversation(conversation);
            m.setSender(sender);
            m.setContent(""); // Empty content for image messages
            m.setMessageType("IMAGE");
            m.setImageUrl(imageUrl);
            Message saved = messageRepository.save(m);

            // Create DTO
            ChatMessageDTO dto = new ChatMessageDTO(
                    saved.getId(),
                    conversationId,
                    null,
                    senderId,
                    saved.getContent(),
                    saved.getMessageType(),
                    saved.getImageUrl(),
                    null, // fileUrl
                    null, // fileName
                    null, // fileSize
                    saved.getCreatedAt()
            );

            // Publish to conversation topic
            messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send image message: " + e.getMessage(), e);
        }
    }

    public ChatMessageDTO sendFileMessage(UUID conversationId, UUID senderId, MultipartFile file) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            // Upload file to Cloudinary
            String fileUrl = fileUploadService.uploadFile(file, "file_" + UUID.randomUUID());

            // Create message
            Message m = new Message();
            m.setConversation(conversation);
            m.setSender(sender);
            m.setContent(""); // Empty content for file messages
            m.setMessageType("FILE");
            m.setFileUrl(fileUrl);
            m.setFileName(file.getOriginalFilename());
            m.setFileSize(file.getSize());
            Message saved = messageRepository.save(m);

            // Create DTO
            ChatMessageDTO dto = new ChatMessageDTO(
                    saved.getId(),
                    conversationId,
                    null,
                    senderId,
                    saved.getContent(),
                    saved.getMessageType(),
                    null, // imageUrl
                    saved.getFileUrl(),
                    saved.getFileName(),
                    saved.getFileSize(),
                    saved.getCreatedAt()
            );

            // Publish to conversation topic
            messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send file message: " + e.getMessage(), e);
        }
    }
}
