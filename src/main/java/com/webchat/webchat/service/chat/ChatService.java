package com.webchat.webchat.service.chat;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.dto.ConversationDTO;
import com.webchat.webchat.entity.Conversation;
import com.webchat.webchat.entity.Message;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.ConversationRepository;
import com.webchat.webchat.repository.MessageRepository;
import com.webchat.webchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

    public Conversation ensureConversation(UUID userAId, UUID userBId) {
        User a = userRepository.findById(userAId).orElseThrow();
        User b = userRepository.findById(userBId).orElseThrow();
        return conversationRepository.findBetween(a, b).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setParticipant1(a);
            c.setParticipant2(b);
            Conversation saved = conversationRepository.save(c);
            // notify both users of new conversation
            ConversationDTO dto = new ConversationDTO(saved.getId(), a.getIdUser(), b.getIdUser());
            messagingTemplate.convertAndSend("/topic/conversations/" + a.getIdUser(), dto);
            messagingTemplate.convertAndSend("/topic/conversations/" + b.getIdUser(), dto);
            return saved;
        });
    }

    public ChatMessageDTO sendMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        User sender = userRepository.findById(senderId).orElseThrow();
        Message m = new Message();
        m.setConversation(conversation);
        m.setSender(sender);
        m.setContent(content);
        Message saved = messageRepository.save(m);

        ChatMessageDTO dto = new ChatMessageDTO(saved.getId(), conversationId, senderId, content, saved.getCreatedAt());
        // publish to conversation topic
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, dto);
        return dto;
    }

    public List<ChatMessageDTO> getMessages(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(m -> new ChatMessageDTO(m.getId(), conversationId, m.getSender().getIdUser(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getConversationsForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return conversationRepository.findByParticipant1OrParticipant2(user, user)
                .stream()
                .map(c -> new ConversationDTO(c.getId(), c.getParticipant1().getIdUser(), c.getParticipant2().getIdUser()))
                .collect(Collectors.toList());
    }
}
