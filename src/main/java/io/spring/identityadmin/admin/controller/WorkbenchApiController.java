package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.domain.dto.GrantRequestDto;
import io.spring.identityadmin.domain.dto.PageResponseDto;
import io.spring.identityadmin.domain.dto.ResourceSearchCriteria;
import io.spring.identityadmin.domain.dto.RevokeRequestDto;
import io.spring.identityadmin.entity.ManagedResource;
import io.spring.identityadmin.iamw.AccessGrantService;
import io.spring.identityadmin.iamw.AccessInquiryService;
import io.spring.identityadmin.iamw.ResourceRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workbench")
@RequiredArgsConstructor
public class WorkbenchApiController {

    private final ResourceRegistryService resourceRegistryService;
    private final AccessInquiryService accessInquiryService;
    private final AccessGrantService accessGrantService;

    @GetMapping("/resources")
    public ResponseEntity<?> findResources(@RequestParam(required = false) String keyword, Pageable pageable) {
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
    public ResponseEntity<?> getEntitlementsByResource(@RequestParam Long resourceId) {
        return ResponseEntity.ok(accessInquiryService.getEntitlementsForResource(resourceId));
    }

    @GetMapping("/entitlements/by-subject")
    public ResponseEntity<?> getEntitlementsBySubject(@RequestParam String type, @RequestParam Long id) {
        return ResponseEntity.ok(accessInquiryService.getEntitlementsForSubject(id, type));
    }

    @PostMapping("/grants")
    public ResponseEntity<?> grantAccess(@RequestBody GrantRequestDto grantRequest) {
        return ResponseEntity.ok(accessGrantService.grantAccess(grantRequest));
    }

    @DeleteMapping("/revocations")
    public ResponseEntity<Void> revokeAccess(@RequestBody RevokeRequestDto revokeRequest) {
        accessGrantService.revokeAccess(revokeRequest);
        return ResponseEntity.noContent().build();
    }
}
