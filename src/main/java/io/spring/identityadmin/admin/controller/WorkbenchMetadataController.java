package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workbench/metadata")
@RequiredArgsConstructor
public class WorkbenchMetadataController {

    private final BusinessMetadataService businessMetadataService;

    @GetMapping("/subjects")
    public ResponseEntity<?> getSubjects() {
        return ResponseEntity.ok(Map.of(
                "users", businessMetadataService.getAllUsersForPolicy(),
                "groups", businessMetadataService.getAllGroupsForPolicy()
        ));
    }

    @GetMapping("/actions")
    public ResponseEntity<?> getActions(@RequestParam Long resourceId) {
        return ResponseEntity.ok(businessMetadataService.getActionsForResource(resourceId));
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(businessMetadataService.getAllRoles()); // BusinessMetadataService에 추가 필요
    }
}
