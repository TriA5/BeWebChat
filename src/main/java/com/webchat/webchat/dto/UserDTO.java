package com.webchat.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserDTO {
    private UUID idUser;
    // private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String avatar;
    private LocalDate dateOfBirth; 
    private boolean gender;
}

