package com.webchat.webchat.repository;

import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.UUID;

@RepositoryRestResource(path = "group-conversations")
public interface GroupConversationRepository extends JpaRepository<GroupConversation, UUID> {
    List<GroupConversation> findByCreatedBy(User createdBy);
}