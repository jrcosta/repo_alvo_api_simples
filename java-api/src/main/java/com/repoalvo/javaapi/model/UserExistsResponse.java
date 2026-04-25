package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserExistsResponse(boolean exists, Integer userId) {

    public UserExistsResponse(boolean exists) {
        this(exists, null);
    }
}
