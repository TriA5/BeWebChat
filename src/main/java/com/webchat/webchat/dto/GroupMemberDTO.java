package com.webchat.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private UUID id;        // GroupMember ID
    private UUID userId;
    private String username;
    private String avatar;
    private String role;    // "ADMIN" or "MEMBER"
}
