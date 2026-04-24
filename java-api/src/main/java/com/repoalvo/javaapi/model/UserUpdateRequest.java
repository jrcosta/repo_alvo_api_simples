package com.repoalvo.javaapi.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 3, max = 100) String name,
        @Email String email
) {
}
