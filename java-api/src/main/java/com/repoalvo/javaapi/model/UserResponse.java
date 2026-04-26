package com.repoalvo.javaapi.model;

public record UserResponse(int id, String name, String email, String status, String role, String phoneNumber, boolean vip) {

    public UserResponse(int id, String name, String email, String status, String role) {
        this(id, name, email, status, role, null);
    }

    public UserResponse(int id, String name, String email, String status, String role, String phoneNumber) {
        this(id, name, email, status, role, phoneNumber, "ADMIN".equals(role));
    }
}
