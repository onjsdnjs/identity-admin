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
        // 사용자, 그룹 목록을 반환
        return ResponseEntity.ok(Map.of(
                "users", businessMetadataService.getAllUsersForPolicy(),
                "groups", businessMetadataService.getAllGroupsForPolicy()
        ));
    }

    @GetMapping("/actions")
    public ResponseEntity<?> getActions(@RequestParam(required = false) Long resourceId) {
        // 특정 리소스에 대한 행위 또는 전체 행위 목록 반환
        if (resourceId != null) {
            return ResponseEntity.ok(businessMetadataService.getActionsForResource(resourceId));
        }
        return ResponseEntity.ok(businessMetadataService.getAllBusinessActions());
    }
}
