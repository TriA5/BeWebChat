package com.webchat.webchat.service.user;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.webchat.webchat.dto.UserProfileDTO;
import com.webchat.webchat.dto.UserRegisterDTO;
import com.webchat.webchat.entity.User;

public interface UserService {
    public ResponseEntity<?> register(UserRegisterDTO dto);
    //Thay đổi ảnh đại diện
    public ResponseEntity<?> changeAvatar(JsonNode userJson);
    //Update Profile
    public ResponseEntity<?> updateProfile(UserProfileDTO dto);
    //Update password
    public ResponseEntity<?> forgotPassword(JsonNode jsonNode);


    public ResponseEntity<?> changePassword(JsonNode userJson);

   
}
