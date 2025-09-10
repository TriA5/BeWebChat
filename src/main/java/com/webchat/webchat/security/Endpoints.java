package com.webchat.webchat.security;

public class Endpoints {
    public static final String font_end_host = "http://localhost:3000";
    public static final String[] PUBLIC_GET = {
        "/user/active-account",
    }; 
    public static final String[] PUBLIC_POST = {
        "/user/register",
        "/user/authenticate"
    };
    public static final String[] PUBLIC_PUT = {

    };
    public static final String[] PUBLIC_DELETE = {

    };
    public static final String[] ADMIN_ENDPOINT = {
        "/admin/**"
    };
}
