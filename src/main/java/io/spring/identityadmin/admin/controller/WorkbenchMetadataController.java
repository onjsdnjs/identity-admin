package io.spring.identityadmin.admin.controller;

import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.domain.dto.BusinessActionDto;
import io.spring.identityadmin.domain.dto.RoleMetadataDto;
import io.spring.identityadmin.entity.BusinessAction;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workbench/metadata")
@RequiredArgsConstructor
public class WorkbenchMetadataController {

    private final BusinessMetadataService businessMetadataService;
    private final ModelMapper modelMapper;

    @GetMapping("/subjects")
    public ResponseEntity<Map<String, List<?>>> getSubjects() {
        return ResponseEntity.ok(Map.of(
                "users", businessMetadataService.getAllUsersForPolicy(),
                "groups", businessMetadataService.getAllGroupsForPolicy()
        ));
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
}
