package com.webchat.webchat.service.friendship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.webchat.webchat.dto.FriendRequestNotification;
import com.webchat.webchat.dto.UserDTO;
import com.webchat.webchat.entity.Friendship;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.FriendshipRepository;
import com.webchat.webchat.repository.UserRepository;
import com.webchat.webchat.service.chat.ChatService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatService chatService;

    public UserDTO searchByPhone(String phone) {
    User user = userRepository.findByPhoneNumberAndEnabledTrue(phone)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy số điện thoại hoặc tài khoản đã bị vô hiệu hóa: " + phone));

    return new UserDTO(
        user.getIdUser(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getAvatar(),
        user.getDateOfBirth(),
        user.getGender()
    );
}

    @Override
public Friendship sendFriendRequest(UUID requesterId, UUID addresseeId) {
    User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new RuntimeException("Người gửi không tồn tại"));
    User addressee = userRepository.findById(addresseeId)
            .orElseThrow(() -> new RuntimeException("Người nhận không tồn tại"));

    // Kiểm tra trùng lời mời hoặc đã là bạn bè
    Optional<Friendship> existing1 = friendshipRepository.findByRequesterAndAddressee(requester, addressee);
    Optional<Friendship> existing2 = friendshipRepository.findByRequesterAndAddressee(addressee, requester);

    if (existing1.isPresent() && !"REJECTED".equals(existing1.get().getStatus()) ||
        existing2.isPresent() && !"REJECTED".equals(existing2.get().getStatus())) {
        throw new RuntimeException("Đã tồn tại lời mời hoặc đã là bạn bè");
    }

    Friendship friendship = new Friendship();
    friendship.setRequester(requester);
    friendship.setAddressee(addressee);
    friendship.setStatus("PENDING");

    Friendship saved = friendshipRepository.save(friendship);

    // gửi socket cho B (addressee)
    FriendRequestNotification notification = new FriendRequestNotification(
            saved.getId(),
            requester.getIdUser(),
            requester.getUsername(),
            "PENDING"
    );
    messagingTemplate.convertAndSend("/topic/friend-requests/" + addressee.getIdUser(), notification);

    return saved;
}

//lấy danh sách lời mời kết bạn
@Override
public List<Friendship> getFriendships(UUID userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

    // Lấy tất cả Friendship mà user là requester hoặc addressee
    return friendshipRepository.findByRequesterOrAddressee(user, user);
}

//lấy danh sách lời mời kết bạn
@Override
public Friendship respondToRequest(UUID friendshipId, String action) {
    Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));

    if ("ACCEPT".equalsIgnoreCase(action)) {
        friendship.setStatus("ACCEPTED");
    } else if ("REJECT".equalsIgnoreCase(action)) {
        friendship.setStatus("REJECTED");
    } else {
        throw new IllegalArgumentException("Action không hợp lệ");
    }

    Friendship saved = friendshipRepository.save(friendship);

    // gửi socket cho cả 2
    FriendRequestNotification notification = new FriendRequestNotification(
            saved.getId(),
            saved.getRequester().getIdUser(),
            saved.getRequester().getUsername(),
            saved.getStatus()
    );

    messagingTemplate.convertAndSend("/topic/friend-requests/" + saved.getRequester().getIdUser(), notification);
    messagingTemplate.convertAndSend("/topic/friend-requests/" + saved.getAddressee().getIdUser(), notification);

    // If accepted, ensure a conversation exists and notify via /topic/conversations/{userId}
    if ("ACCEPTED".equals(saved.getStatus())) {
        chatService.ensureConversation(saved.getRequester().getIdUser(), saved.getAddressee().getIdUser());
    }

    return saved;
}


    @Override
public List<User> getFriends(UUID userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

    // Lấy tất cả friendships mà status = "ACCEPTED"
    List<Friendship> sent = friendshipRepository.findByStatusAndRequester("ACCEPTED", user);
    List<Friendship> received = friendshipRepository.findByStatusAndAddressee("ACCEPTED", user);

    // Ghép thành danh sách User bạn bè
    List<User> friends = new ArrayList<>();

    for(Friendship f : sent) {
        friends.add(f.getAddressee()); // Người nhận là bạn bè
    }
    for(Friendship f : received) {
        friends.add(f.getRequester()); // Người gửi là bạn bè
    }

    return friends;
}

}
