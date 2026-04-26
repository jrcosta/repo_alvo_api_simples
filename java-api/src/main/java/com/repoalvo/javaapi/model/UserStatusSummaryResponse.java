package com.repoalvo.javaapi.model;

import java.util.Map;

public record UserStatusSummaryResponse(Map<String, Long> statuses) {
}
