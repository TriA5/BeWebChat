package com.webchat.webchat.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FriendRequestNotification {
    private UUID friendshipId;
    private UUID fromUserId;
    private String fromUserName;
    private String status; // PENDING | ACCEPTED

}
