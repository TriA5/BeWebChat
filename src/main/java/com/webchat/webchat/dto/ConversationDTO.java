package com.webchat.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private UUID id;
    private UUID participant1Id;
    private UUID participant2Id;
}
