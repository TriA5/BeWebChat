package com.webchat.webchat.service.user;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.webchat.webchat.dto.UserRegisterDTO;
import com.webchat.webchat.entity.User;

public interface UserService {
    public ResponseEntity<?> register(UserRegisterDTO dto);
    //Thay đổi ảnh đại diện
    public ResponseEntity<?> changeAvatar(JsonNode userJson);

   
}
