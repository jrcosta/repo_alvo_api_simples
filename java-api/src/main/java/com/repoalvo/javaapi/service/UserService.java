package com.repoalvo.javaapi.service;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {

    private final List<UserResponse> users = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(3);

    public UserService() {
        users.add(new UserResponse(1, "Ana Silva", "ana@example.com"));
        users.add(new UserResponse(2, "Bruno Lima", "bruno@example.com"));
    }

    public synchronized List<UserResponse> listUsers(int limit, int offset) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, limit);

        if (safeOffset >= users.size()) {
            return List.of();
        }

        int end = Math.min(users.size(), safeOffset + safeLimit);
        return new ArrayList<>(users.subList(safeOffset, end));
    }

    public synchronized List<UserResponse> listAllUsers() {
        return new ArrayList<>(users);
    }

    public synchronized Optional<UserResponse> getById(int userId) {
        return users.stream().filter(u -> u.id() == userId).findFirst();
    }

    public synchronized Optional<UserResponse> findByEmail(String email) {
        return users.stream().filter(u -> u.email().equals(email)).findFirst();
    }

    public synchronized UserResponse create(UserCreateRequest payload) {
        UserResponse user = new UserResponse(nextId.getAndIncrement(), payload.name(), payload.email());
        users.add(user);
        return user;
    }
}
