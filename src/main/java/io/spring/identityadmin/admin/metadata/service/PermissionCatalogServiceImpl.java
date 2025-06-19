package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.dto.PermissionDto;
import io.spring.identityadmin.domain.dto.PolicyDto;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.domain.entity.Permission;
import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.repository.PermissionRepository;
import io.spring.identityadmin.security.xacml.pap.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCatalogServiceImpl implements PermissionCatalogService {

    private final PermissionRepository permissionRepository;
    private final ModelMapper modelMapper;
    private final PolicyService policyService;

    @Override
    @Transactional
    public Permission synchronizePermissionFor(ManagedResource resource) {
        if (resource.getStatus() == ManagedResource.Status.NEEDS_DEFINITION) {
            throw new IllegalStateException("정의가 필요한 리소스로부터 권한을 생성할 수 없습니다. 리소스 ID: " + resource.getId());
        }

        String permissionName = generatePermissionName(resource);

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseGet(() -> Permission.builder().name(permissionName).build());

        permission.setFriendlyName(resource.getFriendlyName());
        permission.setDescription(resource.getDescription());
        permission.setTargetType(resource.getResourceType().name());

        String actionType = "EXECUTE"; // 메서드 기반일 때 기본값
        if (resource.getResourceType() == ManagedResource.ResourceType.URL && resource.getHttpMethod() != null) {
            actionType = resource.getHttpMethod().name();
        }
        permission.setActionType(actionType);
        permission.setManagedResource(resource);

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Permission '{}' has been synchronized for resource '{}'.", savedPermission.getName(), resource.getResourceIdentifier());

        // [핵심] 권한이 생성/업데이트된 후, 이 권한에 대한 정책 동기화를 즉시 호출합니다.
        policyService.synchronizePolicyForPermission(savedPermission);

        return savedPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAvailablePermissions() {
        return permissionRepository.findDefinedPermissionsWithDetails().stream()
                .map(p -> modelMapper.map(p, PermissionDto.class))
                .collect(Collectors.toList());
    }

    /**
     * ManagedResource를 기반으로 가독성이 높은 고유 권한 이름을 생성합니다.
     * - 메서드: 패키지 경로를 제외하고 '클래스명_메서드명' 형태로 생성합니다.
     * - URL: 경로 변수와 특수문자를 정리하여 'ADMIN_USERS_ID'와 같은 형태로 생성합니다.
     */
    private String generatePermissionName(ManagedResource resource) {
        String typePrefix = resource.getResourceType().name();
        String identifierPart;

        if (resource.getResourceType() == ManagedResource.ResourceType.METHOD) {
            String fullIdentifier = resource.getResourceIdentifier();
            // 파라미터 부분 "()" 제거
            String withoutParams = fullIdentifier.replaceAll("\\(.*\\)", "");
            // '.' 기준으로 분리
            String[] parts = withoutParams.split("\\.");
            if (parts.length >= 2) {
                // 마지막 두 부분(클래스명, 메서드명)만 사용
                String className = parts[parts.length - 2];
                String methodName = parts[parts.length - 1];
                identifierPart = (className + "_" + methodName).toUpperCase();
            } else {
                // 예외적인 경우, 기존 방식과 유사하게 처리
                identifierPart = withoutParams.replace('.', '_').toUpperCase();
            }
        } else { // URL 타입의 경우
            identifierPart = resource.getResourceIdentifier()
                    // 경로 변수(예: {id})를 'ID' 문자열로 대체
                    .replaceAll("\\{.*?\\}", "ID")
                    // 슬래시(/)를 언더스코어(_)로 변경
                    .replace('/', '_')
                    // 허용된 문자(알파벳, 숫자, _)를 제외한 모든 문자 제거
                    .replaceAll("[^a-zA-Z0-9_]", "")
                    .toUpperCase();
        }

        // 공통 후처리: 연속된 언더스코어를 하나로, 시작/끝 언더스코어는 제거
        identifierPart = identifierPart.replaceAll("_+", "_").replaceAll("^_|_$", "");

        return String.format("%s_%s", typePrefix, identifierPart);
    }
}