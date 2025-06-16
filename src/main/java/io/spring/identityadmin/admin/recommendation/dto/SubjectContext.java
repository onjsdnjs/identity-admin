package io.spring.identityadmin.admin.recommendation.dto;

import java.util.Set;

public record SubjectContext(Long subjectId, String subjectType, Set<Long> groupIds, Set<String> roles) {}
