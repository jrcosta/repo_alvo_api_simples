package com.repoalvo.javaapi.model;

public record CountResponse(int count, String resource) {

    public CountResponse(int count) {
        this(count, "users");
    }
}
