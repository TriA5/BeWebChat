package com.webchat.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicUserDTO {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String avatar;
}
