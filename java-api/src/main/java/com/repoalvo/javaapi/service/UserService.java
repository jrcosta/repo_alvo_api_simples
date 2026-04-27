package com.repoalvo.javaapi.service;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserUpdateRequest;
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
        reset();
    }

    public synchronized void reset() {
        users.clear();
        users.add(new UserResponse(1, "Ana Silva", "ana@example.com", "ACTIVE", "ADMIN", "+55 11 90000-0001"));
        users.add(new UserResponse(2, "Bruno Lima", "bruno@example.com", "ACTIVE", "USER", "+55 11 90000-0002"));
        nextId.set(3);
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
        String role = (payload.role() != null) ? payload.role() : "USER";
        UserResponse user = new UserResponse(
                nextId.getAndIncrement(),
                payload.name(),
                payload.email(),
                "ACTIVE",
                role,
                payload.phoneNumber()
        );
        users.add(user);
        return user;
    }

    public synchronized Optional<UserResponse> update(int userId, UserUpdateRequest payload) {
        for (int i = 0; i < users.size(); i++) {
            UserResponse existing = users.get(i);
            if (existing.id() == userId) {
                String updatedName = (payload.name() != null) ? payload.name() : existing.name();
                String updatedEmail = (payload.email() != null) ? payload.email() : existing.email();
                String updatedRole = (payload.role() != null) ? payload.role() : existing.role();
                String updatedPhone = (payload.phoneNumber() != null) ? payload.phoneNumber() : existing.phoneNumber();

                UserResponse updated = new UserResponse(
                        existing.id(),
                        updatedName,
                        updatedEmail,
                        existing.status(),
                        updatedRole,
                        updatedPhone
                );
                users.set(i, updated);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }
    public synchronized UserResponse update(int userId, UserCreateRequest payload) {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                payload.name(),
                payload.email(),
                payload.role(),
                payload.phoneNumber()
        );
        return update(userId, updateRequest).orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public synchronized void delete(int userId) {
        users.removeIf(u -> u.id() == userId);
    }

    public synchronized List<UserResponse> searchByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return List.of();
        return users.stream()
                .filter(u -> phoneNumber.equals(u.phoneNumber()))
                .toList();
    }
}

