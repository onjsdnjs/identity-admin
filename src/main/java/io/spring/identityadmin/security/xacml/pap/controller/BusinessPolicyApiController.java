package io.spring.identityadmin.security.xacml.pap.controller;

import io.spring.identityadmin.domain.dto.BusinessPolicyDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.security.xacml.pap.service.BusinessPolicyService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/authoring")
@RequiredArgsConstructor
public class BusinessPolicyApiController {
    private final BusinessPolicyService businessPolicyService;
    private final ModelMapper modelMapper;

    @PostMapping("/policies")
    public ResponseEntity<PolicyDto> createPolicy(@RequestBody BusinessPolicyDto businessPolicyDto) {
        Policy policy = businessPolicyService.createPolicyFromBusinessRule(businessPolicyDto);
        return ResponseEntity.ok(modelMapper.map(policy, PolicyDto.class));
    }

    @PutMapping("/policies/{id}")
    public ResponseEntity<PolicyDto> updatePolicy(@PathVariable Long id, @RequestBody BusinessPolicyDto businessPolicyDto) {
        Policy policy = businessPolicyService.updatePolicyFromBusinessRule(id, businessPolicyDto);
        return ResponseEntity.ok(modelMapper.map(policy, PolicyDto.class));
    }
}