package com.webchat.webchat.security;

public class Endpoints {
    public static final String font_end_host = "http://localhost:3000";
    public static final String[] PUBLIC_GET = {
        "/users/**",
        "/user/active-account",
        "/friendships/search/**", // đúng với API bạn gọi
        "/friendships/**",
        "/ws/**",
        "/chat/**",
        

    }; 
    public static final String[] PUBLIC_POST = {
        "/user/register",
        "/user/authenticate",
        "/friendships/send/**",
        "/friendships/**",
        "/ws/**",
        "/chat/**",
    };
    public static final String[] PUBLIC_PUT = {
        "/user/change-avatar",
        "/user/update-profile",
        "/user/forgot-password",
        "/user/change-password",
    };
    public static final String[] PUBLIC_DELETE = {

    };
    public static final String[] ADMIN_ENDPOINT = {
        "/admin/**"
    };
}
