package io.spring.identityadmin.workflow.wizard.dto;

import java.util.Set;

public record SaveSubjectsRequest(Set<Long> userIds, Set<Long> groupIds) {}
