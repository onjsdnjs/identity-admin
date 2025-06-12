package io.spring.identityadmin.security.xacml.pap.dto;

import java.util.List;
public record DuplicatePolicyDto(String reason, List<Long> policyIds) {}