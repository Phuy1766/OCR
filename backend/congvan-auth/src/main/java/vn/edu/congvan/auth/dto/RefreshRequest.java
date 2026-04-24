package vn.edu.congvan.auth.dto;

/** Refresh request — token có thể đến từ body hoặc HttpOnly cookie. */
public record RefreshRequest(String refreshToken) {}
