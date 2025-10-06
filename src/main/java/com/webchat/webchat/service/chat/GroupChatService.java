package com.webchat.webchat.service.chat;

import com.webchat.webchat.dto.ChatMessageDTO;
import com.webchat.webchat.dto.GroupConversationDTO;
import com.webchat.webchat.entity.GroupConversation;
import com.webchat.webchat.entity.GroupMember;
import com.webchat.webchat.entity.Message;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.GroupConversationRepository;
import com.webchat.webchat.repository.GroupMemberRepository;
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
public class GroupChatService {
    private final GroupConversationRepository groupConversationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UploadImageService uploadImageService;
    private final FileUploadService fileUploadService;

    public GroupConversation createGroup(UUID creatorId, String groupName, List<UUID> initialMemberIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Người tạo không tồn tại"));
        GroupConversation group = new GroupConversation();
        group.setName(groupName);
        group.setCreatedBy(creator);
        GroupConversation savedGroup = groupConversationRepository.save(group);

        // Thêm creator là ADMIN
        addMember(savedGroup, creator, "ADMIN");

        // Thêm initial members là MEMBER
        for (UUID memberId : initialMemberIds) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + memberId));
            addMember(savedGroup, member, "MEMBER");
        }

        // Notify tất cả thành viên về group mới
        GroupConversationDTO dto = mapToDTO(savedGroup);
        for (GroupMember gm : groupMemberRepository.findByGroup(savedGroup)) {
            messagingTemplate.convertAndSend("/topic/groups/" + gm.getUser().getIdUser(), dto);
        }

        return savedGroup;
    }

    public void joinGroup(UUID groupId, UUID userId) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        if (groupMemberRepository.findByGroupAndUser(group, user).isPresent()) {
            throw new RuntimeException("Đã là thành viên của nhóm");
        }
        addMember(group, user, "MEMBER");

        // Notify nhóm về thành viên mới
        messagingTemplate.convertAndSend("/topic/group/" + groupId, "User " + user.getUsername() + " đã tham gia");
    }

    public ChatMessageDTO sendGroupMessage(UUID groupId, UUID senderId, String content) {
    try {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        groupMemberRepository.findByGroupAndUser(group, sender)
                .orElseThrow(() -> new RuntimeException("Không phải thành viên của nhóm"));

        Message m = new Message();
        m.setGroupConversation(group);
        m.setSender(sender);
        m.setContent(content);
        m.setMessageType("TEXT");
        Message saved = messageRepository.save(m);

        ChatMessageDTO dto = new ChatMessageDTO(saved.getId(), null, groupId, senderId, content, "TEXT", null, null, null, null, saved.getCreatedAt());
        messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
        return dto;
    } catch (Exception e) {
        System.err.println("Lỗi khi gửi tin nhắn nhóm: " + e.getMessage());
        e.printStackTrace();
        throw e; // Ném lại để debug
    }
}
    public List<ChatMessageDTO> getGroupMessages(UUID groupId) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        return messageRepository.findByGroupConversationOrderByCreatedAtAsc(group)
                .stream()
                .map(m -> new ChatMessageDTO(
                    m.getId(), 
                    null, 
                    groupId, 
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

    public List<GroupConversationDTO> getGroupsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return groupMemberRepository.findByUser(user)
                .stream()
                .map(gm -> mapToDTO(gm.getGroup()))
                .collect(Collectors.toList());
    }

    private void addMember(GroupConversation group, User user, String role) {
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setUser(user);
        gm.setRole(role);
        groupMemberRepository.save(gm);
    }

    public void removeMember(UUID groupId, UUID userId, UUID requesterId) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Người yêu cầu không tồn tại"));

        // Kiểm tra quyền: phải là ADMIN hoặc chính user đó (tự rời nhóm)
        GroupMember requesterMember = groupMemberRepository.findByGroupAndUser(group, requester)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm"));
        
        if (!requesterId.equals(userId) && !"ADMIN".equals(requesterMember.getRole())) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền xóa thành viên");
        }

        GroupMember memberToRemove = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new RuntimeException("Người dùng không phải thành viên của nhóm"));

        // Không cho phép xóa creator
        if (group.getCreatedBy().getIdUser().equals(userId)) {
            throw new RuntimeException("Không thể xóa người tạo nhóm");
        }

        groupMemberRepository.delete(memberToRemove);

        // Broadcast notification
        String message = user.getUsername() + (requesterId.equals(userId) ? " đã rời khỏi nhóm" : " đã bị xóa khỏi nhóm");
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/member-removed", 
            new MemberRemovedNotification(groupId, userId, message));
    }

    public List<com.webchat.webchat.dto.GroupMemberDTO> getGroupMembers(UUID groupId) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        return members.stream()
                .map(gm -> {
                    User u = gm.getUser();
                    return new com.webchat.webchat.dto.GroupMemberDTO(
                            gm.getId(),
                            u.getIdUser(),
                            u.getUsername(),
                            u.getAvatar(),
                            gm.getRole()
                    );
                })
                .collect(Collectors.toList());
    }

    public void deleteGroup(UUID groupId, UUID requesterId) {
        GroupConversation group = groupConversationRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
        
        // Chỉ creator mới có thể xóa nhóm
        if (!group.getCreatedBy().getIdUser().equals(requesterId)) {
            throw new RuntimeException("Chỉ người tạo nhóm mới có thể xóa nhóm");
        }

        // Lấy tất cả thành viên trước khi xóa
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        List<UUID> memberIds = members.stream()
                .map(gm -> gm.getUser().getIdUser())
                .collect(Collectors.toList());

        // Xóa tất cả tin nhắn trong nhóm
        List<Message> messages = messageRepository.findByGroupConversationOrderByCreatedAtAsc(group);
        messageRepository.deleteAll(messages);

        // Xóa tất cả thành viên
        groupMemberRepository.deleteAll(members);

        // Xóa nhóm
        groupConversationRepository.delete(group);

        // Thông báo cho tất cả thành viên về việc nhóm bị xóa
        GroupDeletedNotification notification = new GroupDeletedNotification(
                groupId,
                group.getName(),
                "Nhóm đã bị xóa bởi người tạo"
        );
        
        for (UUID memberId : memberIds) {
            messagingTemplate.convertAndSend("/topic/groups/" + memberId, notification);
        }
    }

    private GroupConversationDTO mapToDTO(GroupConversation group) {
        return new GroupConversationDTO(group.getId(), group.getName(), group.getCreatedBy().getIdUser());
    }

    // DTO for notification
    public static class MemberRemovedNotification {
        public UUID groupId;
        public UUID userId;
        public String message;
        public MemberRemovedNotification(UUID groupId, UUID userId, String message) {
            this.groupId = groupId;
            this.userId = userId;
            this.message = message;
        }
    }

    public static class GroupDeletedNotification {
        public UUID groupId;
        public String groupName;
        public String message;
        public GroupDeletedNotification(UUID groupId, String groupName, String message) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.message = message;
        }
    }

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

            // Create message
            Message m = new Message();
            m.setGroupConversation(group);
            m.setSender(sender);
            m.setContent(""); // Empty content for image messages
            m.setMessageType("IMAGE");
            m.setImageUrl(imageUrl);
            Message saved = messageRepository.save(m);

            // Create DTO
            ChatMessageDTO dto = new ChatMessageDTO(
                    saved.getId(),
                    null,
                    groupId,
                    senderId,
                    saved.getContent(),
                    saved.getMessageType(),
                    saved.getImageUrl(),
                    null, // fileUrl
                    null, // fileName
                    null, // fileSize
                    saved.getCreatedAt()
            );

            // Publish to group topic
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send group image message: " + e.getMessage(), e);
        }
    }

    public ChatMessageDTO sendGroupFileMessage(UUID groupId, UUID senderId, MultipartFile file) {
        try {
            GroupConversation group = groupConversationRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            groupMemberRepository.findByGroupAndUser(group, sender)
                    .orElseThrow(() -> new RuntimeException("Không phải thành viên của nhóm"));

            // Upload file to Cloudinary
            String fileUrl = fileUploadService.uploadFile(file, "group_file_" + UUID.randomUUID());

            // Create message
            Message m = new Message();
            m.setGroupConversation(group);
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
                    null,
                    groupId,
                    senderId,
                    saved.getContent(),
                    saved.getMessageType(),
                    null, // imageUrl
                    saved.getFileUrl(),
                    saved.getFileName(),
                    saved.getFileSize(),
                    saved.getCreatedAt()
            );

            // Publish to group topic
            messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send group file message: " + e.getMessage(), e);
        }
    }
}