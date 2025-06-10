package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.domain.dto.*;
import io.spring.identityadmin.entity.ManagedResource;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.iamw.AccessGrantService;
import io.spring.identityadmin.iamw.AccessInquiryService;
import io.spring.identityadmin.iamw.ResourceRegistryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workbench")
@RequiredArgsConstructor
public class WorkbenchApiController {

    private final ResourceRegistryService resourceRegistryService;
    private final AccessInquiryService accessInquiryService;
    private final AccessGrantService accessGrantService;
    private final ModelMapper modelMapper;

    @GetMapping("/resources")
    public ResponseEntity<PageResponseDto<ManagedResource>> findResources(@RequestParam(required = false) String keyword, Pageable pageable) {
        ResourceSearchCriteria criteria = new ResourceSearchCriteria();
        criteria.setKeyword(keyword);
        Page<ManagedResource> resourcePage = resourceRegistryService.findResources(criteria, pageable);
        return ResponseEntity.ok(new PageResponseDto<>(resourcePage));
    }

    @PostMapping("/resources/refresh")
    public ResponseEntity<Void> refreshResources() {
        resourceRegistryService.refreshResources();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/entitlements/by-resource")
    public ResponseEntity<List<EntitlementDto>> getEntitlementsByResource(@RequestParam Long resourceId) {
        return ResponseEntity.ok(accessInquiryService.getEntitlementsForResource(resourceId));
    }

    @GetMapping("/entitlements/by-subject")
    public ResponseEntity<List<EntitlementDto>> getEntitlementsBySubject(@RequestParam String type, @RequestParam Long id) {
        return ResponseEntity.ok(accessInquiryService.getEntitlementsForSubject(id, type));
    }

    @PostMapping("/grants")
    public ResponseEntity<PolicyDto> grantAccess(@RequestBody GrantRequestDto grantRequest) {
        Policy policy = accessGrantService.grantAccess(grantRequest);
        return ResponseEntity.ok(modelMapper.map(policy, PolicyDto.class));
    }

    @DeleteMapping("/revocations")
    public ResponseEntity<Void> revokeAccess(@RequestBody RevokeRequestDto revokeRequest) {
        accessGrantService.revokeAccess(revokeRequest);
        return ResponseEntity.noContent().build();
    }
}
