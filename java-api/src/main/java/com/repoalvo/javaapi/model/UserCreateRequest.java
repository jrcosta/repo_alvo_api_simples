package com.repoalvo.javaapi.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        @NotBlank @Email String email,
        String role
) {
}
