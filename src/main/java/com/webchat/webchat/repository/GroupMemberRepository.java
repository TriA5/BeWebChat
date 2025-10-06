package com.webchat.webchat.repository;

import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.entity.GroupMember;
import com.webchat.webchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// @RepositoryRestResource(path = "group-members")
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroup(GroupConversation group);
    List<GroupMember> findByUser(User user);
    Optional<GroupMember> findByGroupAndUser(GroupConversation group, User user);
}