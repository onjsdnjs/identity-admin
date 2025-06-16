package io.spring.identityadmin.resource;

import io.spring.identityadmin.domain.dto.ResourceMetadataDto;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceEnhancementService {

    private final ManagedResourceRepository managedResourceRepository;
    private final ResourceRegistryService resourceRegistryService; // 기존 서비스 재사용

    /**
     * 특정 리소스의 메타데이터(사용자 친화적 이름, 설명, 관리 여부)를 업데이트합니다.
     * 이 메서드는 새로운 '기능 카탈로그' 화면의 저장 로직을 처리합니다.
     * @param id 업데이트할 ManagedResource의 ID
     * @param metadataDto 업데이트할 메타데이터 DTO
     */
    @Transactional
    public void updateResourceMetadata(Long id, ResourceMetadataDto metadataDto) {
        ManagedResource resource = managedResourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 리소스를 찾을 수 없습니다: " + id));

        // DTO로부터 받은 값으로 엔티티의 필드를 업데이트합니다.
        resource.setFriendlyName(metadataDto.getFriendlyName());
        resource.setDescription(metadataDto.getDescription());
        resource.setManaged(metadataDto.isManaged());

        // 변경된 엔티티를 저장합니다.
        managedResourceRepository.save(resource);
        log.info("리소스 메타데이터가 업데이트되었습니다. ID: {}, 이름: {}", id, metadataDto.getFriendlyName());
    }

    /**
     * 워크벤치 표시 여부(isManaged) 상태를 일괄적으로 업데이트합니다.
     * '전체 선택/해제' 기능을 지원합니다.
     * @param ids 업데이트할 리소스 ID 목록
     * @param isManaged 워크벤치 표시 여부
     */
    @Transactional
    public void batchUpdateManagedStatus(java.util.List<Long> ids, boolean isManaged) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        java.util.List<ManagedResource> resourcesToUpdate = managedResourceRepository.findAllById(ids);
        resourcesToUpdate.forEach(resource -> resource.setManaged(isManaged));
        managedResourceRepository.saveAll(resourcesToUpdate);
        log.info("{}개의 리소스 상태가 '{}'(으)로 일괄 변경되었습니다.", resourcesToUpdate.size(), isManaged ? "워크벤치에 표시" : "워크벤치에서 숨김");
    }

    /**
     * 시스템의 리소스를 새로고침합니다. 기존 로직을 그대로 호출합니다.
     */
    public void refreshResources() {
        resourceRegistryService.refreshAndSynchronizeResources();
    }
}
