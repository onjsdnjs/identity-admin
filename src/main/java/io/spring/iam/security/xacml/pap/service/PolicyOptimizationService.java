package io.spring.iam.security.xacml.pap.service;

import io.spring.iam.domain.dto.PolicyDto;
import io.spring.iam.security.xacml.pap.dto.DuplicatePolicyDto;

import java.util.List;

public interface PolicyOptimizationService {
    List<DuplicatePolicyDto> findDuplicatePolicies();
    PolicyDto proposeMerge(List<Long> policyIds);
}