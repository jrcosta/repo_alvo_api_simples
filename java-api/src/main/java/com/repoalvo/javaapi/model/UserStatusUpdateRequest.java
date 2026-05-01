package com.repoalvo.javaapi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusUpdateRequest(
        @NotBlank(message = "O campo 'status' é obrigatório")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status inválido. Valores aceitos: ACTIVE, INACTIVE")
        String status
) {}
