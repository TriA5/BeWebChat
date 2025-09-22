package com.webchat.webchat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.JsonNode;
import com.webchat.webchat.dto.UserRegisterDTO;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.repository.UserRepository;
import com.webchat.webchat.security.JwtResponse;
import com.webchat.webchat.security.LoginRequest;
import com.webchat.webchat.service.JWT.JwtService;
import com.webchat.webchat.service.user.UserServiceImp;
import org.springframework.security.core.AuthenticationException;
import jakarta.mail.MessagingException;

@RestController
@CrossOrigin()
@RequestMapping("/user")
public class UserController {
    //
    @Autowired
    private UserServiceImp userServiceImp;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationManager authenticationManager;

    //
    // đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody UserRegisterDTO dto) throws MessagingException {
        return userServiceImp.register(dto);
    }


    @GetMapping("/active-account")
    public ResponseEntity<?> activeAccount(@RequestParam String email, @RequestParam String activationCode) {
        return userServiceImp.activeAccount(email, activationCode);
    }

    // đăng nhập
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            if (authentication.isAuthenticated()) {
                // Lấy user từ DB
                User user = userRepository.findByUsername(loginRequest.getUsername());
                if (user == null) {
                    throw new RuntimeException("Không tìm thấy user");
                }

                // Kiểm tra trạng thái kích hoạt
                if (!user.isEnabled()) {
                    return ResponseEntity
                            .badRequest()
                            .body("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác thực!");
                }

                // Nếu OK thì cấp JWT
                final String jwtToken = jwtService.generateToken(loginRequest.getUsername());
                return ResponseEntity.ok(new JwtResponse(jwtToken));
            }

        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body("Tên đăng nhập hoặc mật khẩu không đúng!");
        }

        return ResponseEntity.badRequest().body("Xác thực không thành công");
    }
    //thay đổi ảnh đại diện
    @PutMapping("/change-avatar")
    public ResponseEntity<?> changeAvatar(@RequestBody JsonNode jsonData) {
        try{
            return userServiceImp.changeAvatar(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
