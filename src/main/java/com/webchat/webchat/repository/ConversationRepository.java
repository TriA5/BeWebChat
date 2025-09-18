package com.webchat.webchat.repository;

import com.webchat.webchat.entity.Conversation;
import com.webchat.webchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("SELECT c FROM Conversation c WHERE (c.participant1 = :u1 AND c.participant2 = :u2) OR (c.participant1 = :u2 AND c.participant2 = :u1)")
    Optional<Conversation> findBetween(User u1, User u2);

    List<Conversation> findByParticipant1OrParticipant2(User p1, User p2);
}
