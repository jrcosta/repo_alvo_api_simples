package com.repoalvo.javaapi.model;

public record UserResponse(int id, String name, String email, String status, String role, String phoneNumber) {

    public UserResponse(int id, String name, String email, String status, String role) {
        this(id, name, email, status, role, null);
    }
}
