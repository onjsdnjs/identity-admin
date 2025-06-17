package io.spring.identityadmin.admin.metadata.controller;

import io.spring.identityadmin.admin.metadata.service.BusinessMetadataService;
import io.spring.identityadmin.admin.metadata.service.PermissionCatalogService;
import io.spring.identityadmin.domain.dto.BusinessActionDto;
import io.spring.identityadmin.domain.dto.RoleMetadataDto;
import io.spring.identityadmin.domain.entity.business.BusinessAction;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workbench/metadata")
@RequiredArgsConstructor
public class WorkbenchMetadataController {

    private final BusinessMetadataService businessMetadataService;
    private final ModelMapper modelMapper;
    private final PermissionCatalogService permissionCatalogService;

    @GetMapping("/subjects")
    public ResponseEntity<Map<String, Object>> getSubjectsForStudio() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", businessMetadataService.getAllUsersForPolicy());
        response.put("groups", businessMetadataService.getAllGroupsForPolicy());
        response.put("roles", businessMetadataService.getAllRoles()); // 역할 데이터 추가
        response.put("permissions", permissionCatalogService.getAvailablePermissions()); // 권한 데이터 추가
        return ResponseEntity.ok(response);
    }

    @GetMapping("/actions")
    public ResponseEntity<List<BusinessActionDto>> getActions(@RequestParam Long resourceId) {
        List<BusinessAction> businessActions = businessMetadataService.getActionsForResource(resourceId);
        return ResponseEntity.ok(businessActions.stream()
                .map(action -> modelMapper.map(action, BusinessActionDto.class))
                .toList());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleMetadataDto>> getRoles() {
        return ResponseEntity.ok(businessMetadataService.getAllRoles()); // BusinessMetadataService에 추가 필요
    }

    @GetMapping("/authoring-metadata")
    public ResponseEntity<Map<String, Object>> getPolicyAuthoringMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("subjects", businessMetadataService.getAllUsersAndGroups());
        metadata.put("resources", businessMetadataService.getAllBusinessResources());
        // 모든 리소스에 대한 모든 액션을 미리 보내기보다, 리소스 선택 시 동적으로 액션을 가져오는 것이 효율적일 수 있음.
        // 여기서는 일단 모든 액션을 보냄.
        metadata.put("actions", businessMetadataService.getAllBusinessActions());
        metadata.put("conditionTemplates", businessMetadataService.getAllConditionTemplates());
        return ResponseEntity.ok(metadata);
    }
}
