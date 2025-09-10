package com.webchat.webchat.service.email;

public interface EmailService {
    public void sendMessage(String from, String to, String subject, String message);
}
