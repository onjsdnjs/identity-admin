package io.spring.identityadmin.workflow.wizard.dto;

import java.util.Set;

public record SavePermissionsRequest(Set<Long> permissionIds) {}
