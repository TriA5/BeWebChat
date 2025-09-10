package com.webchat.webchat.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.webchat.webchat.entity.User;


public interface UserSecurityService extends UserDetailsService {
    public User findByUsername(String username);
}
