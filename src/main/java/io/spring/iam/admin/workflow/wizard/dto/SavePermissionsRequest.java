package io.spring.iam.admin.workflow.wizard.dto;

import java.util.Set;

public record SavePermissionsRequest(Set<Long> permissionIds) {}
