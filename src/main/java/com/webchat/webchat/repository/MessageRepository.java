package com.webchat.webchat.repository;

import com.webchat.webchat.entity.Conversation;
import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    List<Message> findByGroupConversationOrderByCreatedAtAsc(GroupConversation groupConversation);
}
