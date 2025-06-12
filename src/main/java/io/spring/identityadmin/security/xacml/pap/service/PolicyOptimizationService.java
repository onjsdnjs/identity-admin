package io.spring.identityadmin.security.xacml.pap.service;

import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.security.xacml.pap.dto.DuplicatePolicyDto;

import java.util.List;

public interface PolicyOptimizationService {
    List<DuplicatePolicyDto> findDuplicatePolicies();
    PolicyDto proposeMerge(List<Long> policyIds);
}