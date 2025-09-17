package com.webchat.webchat.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.webchat.webchat.dto.UserDTO;
import com.webchat.webchat.entity.Friendship;
import com.webchat.webchat.entity.User;
import com.webchat.webchat.service.friendship.FriendshipService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/friendships")
@RequiredArgsConstructor
public class FriendshipController {
    @Autowired
    private FriendshipService friendshipService;

    // 🔎 Tìm bạn qua số điện thoại
   @GetMapping("/search")
public ResponseEntity<?> searchFriendByPhone(@RequestParam("phone") String phone) {
    try {
        UserDTO dto = friendshipService.searchByPhone(phone);
        return ResponseEntity.ok(dto);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
    // Gửi lời mời kết bạn
    @PostMapping("/send")
    public ResponseEntity<Friendship> sendRequest(@RequestParam UUID requesterId, @RequestParam UUID addresseeId) {
        return ResponseEntity.ok(friendshipService.sendFriendRequest(requesterId, addresseeId));
    }
    // Lấy danh sách lời mời kết bạn
    @GetMapping("/{userId}/friends")
public ResponseEntity<List<User>> getFriends(@PathVariable UUID userId) {
    return ResponseEntity.ok(friendshipService.getFriends(userId));
}

    // Chấp nhận / Từ chối
    @PostMapping("/{id}/respond")
    public ResponseEntity<Friendship> respond(@PathVariable UUID id, @RequestParam String action) {
        return ResponseEntity.ok(friendshipService.respondToRequest(id, action));
    }
    // Lấy tất cả Friendship liên quan đến user (cả người gửi và người nhận)
@GetMapping("/{userId}/friendships")
public ResponseEntity<List<Friendship>> getAllFriendships(@PathVariable UUID userId) {
    try {
        List<Friendship> friendships = friendshipService.getFriendships(userId);
        return ResponseEntity.ok(friendships);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(null);
    }
}
}
