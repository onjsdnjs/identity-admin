package io.spring.iam.admin.support.context.dto;

import java.time.LocalDateTime;

public record RecentActivityDto(String action, String target, LocalDateTime timestamp) {}
