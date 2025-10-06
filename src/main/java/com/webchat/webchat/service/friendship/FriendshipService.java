package com.webchat.webchat.service.friendship;

import java.util.List;
import java.util.UUID;

import com.webchat.webchat.dto.UserDTO;
import com.webchat.webchat.entity.Friendship;
import com.webchat.webchat.entity.User;
public interface FriendshipService {
    UserDTO searchByPhone(String phone);

    Friendship sendFriendRequest(UUID requesterId, UUID addresseeId);

    Friendship respondToRequest(UUID friendshipId, String action); // ACCEPT | REJECT

    List<User> getFriends(UUID userId);

    List<Friendship> getFriendships(UUID userId);

    void unfriend(UUID userId, UUID friendId);

}
