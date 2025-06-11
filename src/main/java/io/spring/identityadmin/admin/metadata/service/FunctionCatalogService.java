package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.dto.FunctionCatalogDto;
import io.spring.identityadmin.domain.dto.FunctionCatalogUpdateDto;
import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.FunctionGroup;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
import io.spring.identityadmin.repository.FunctionGroupRepository;
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
@Transactional(readOnly = true)
public class FunctionCatalogService {

    private final FunctionCatalogRepository functionCatalogRepository;
    private final FunctionGroupRepository functionGroupRepository;
    private final ModelMapper modelMapper;

    public List<FunctionCatalog> findUnconfirmedFunctions() {
        return functionCatalogRepository.findFunctionsByStatusWithDetails(FunctionCatalog.CatalogStatus.UNCONFIRMED);
    }

    public List<FunctionGroup> getAllFunctionGroups() {
        if (functionGroupRepository.count() == 0) {
            functionGroupRepository.save(FunctionGroup.builder().name("일반").build());
            functionGroupRepository.save(FunctionGroup.builder().name("사용자 관리").build());
        }
        return functionGroupRepository.findAll();
    }

    @Transactional
    public void confirmFunction(Long catalogId, Long groupId) {
        FunctionCatalog catalog = functionCatalogRepository.findById(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기능 카탈로그 ID: " + catalogId));
        FunctionGroup group = functionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기능 그룹 ID: " + groupId));

        catalog.setStatus(FunctionCatalog.CatalogStatus.ACTIVE);
        catalog.setFunctionGroup(group);
        functionCatalogRepository.save(catalog);
        log.info("기능이 확인 및 등록되었습니다. [ID: {}, 이름: {}, 그룹: {}]", catalog.getId(), catalog.getFriendlyName(), group.getName());
    }

    // --- Phase 2: 기능 카탈로그 관리 관련 ---

    /**
     * 관리 가능한 모든 카탈로그(활성/비활성)를 DTO로 변환하여 조회합니다.
     * @return List of FunctionCatalogDto
     */
    public List<FunctionCatalogDto> getManageableCatalogs() {
        return functionCatalogRepository.findAllByStatusNotWithDetails(FunctionCatalog.CatalogStatus.UNCONFIRMED).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기능 카탈로그 항목을 업데이트합니다.
     * @param id 카탈로그 ID
     * @param dto 업데이트 정보
     */
    @Transactional
    public void updateCatalog(Long id, FunctionCatalogUpdateDto dto) {
        FunctionCatalog catalog = functionCatalogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기능 카탈로그 ID: " + id));
        FunctionGroup group = functionGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기능 그룹 ID: " + dto.getGroupId()));

        catalog.setFriendlyName(dto.getFriendlyName());
        catalog.setDescription(dto.getDescription());
        catalog.setStatus(dto.getStatus());
        catalog.setFunctionGroup(group);
        functionCatalogRepository.save(catalog);
    }

    /**
     * [신규 및 오류 수정] 개별 기능 카탈로그의 상태를 업데이트합니다.
     * @param catalogId 업데이트할 카탈로그 ID
     * @param status 변경할 상태 문자열 ("ACTIVE" or "INACTIVE")
     */
    @Transactional
    public void updateSingleStatus(Long catalogId, String status) {
        FunctionCatalog catalog = functionCatalogRepository.findById(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기능 카탈로그 ID: " + catalogId));
        FunctionCatalog.CatalogStatus newStatus = FunctionCatalog.CatalogStatus.valueOf(status.toUpperCase());
        catalog.setStatus(newStatus);
        functionCatalogRepository.save(catalog);
        log.info("카탈로그 ID {}의 상태가 {}로 변경되었습니다.", catalogId, newStatus);
    }

    // '권한 정의' 화면을 위한 활성화된 기능 목록 조회
    public List<FunctionCatalog> findAllActiveFunctions() {
        // [오류 수정] 정확한 메소드와 파라미터로 호출
        return functionCatalogRepository.findFunctionsByStatusWithDetails(FunctionCatalog.CatalogStatus.ACTIVE);
    }

    /**
     * 여러 기능 카탈로그의 상태를 일괄 변경합니다.
     * @param ids 카탈로그 ID 목록
     * @param status 변경할 상태 (ACTIVE or INACTIVE)
     */
    @Transactional
    public void batchUpdateStatus(List<Long> ids, String status) {
        FunctionCatalog.CatalogStatus newStatus = FunctionCatalog.CatalogStatus.valueOf(status.toUpperCase());
        List<FunctionCatalog> catalogs = functionCatalogRepository.findAllById(ids);
        catalogs.forEach(catalog -> catalog.setStatus(newStatus));
        functionCatalogRepository.saveAll(catalogs);
    }

    private FunctionCatalogDto convertToDto(FunctionCatalog catalog) {
        FunctionCatalogDto dto = modelMapper.map(catalog, FunctionCatalogDto.class);
        if (catalog.getManagedResource() != null) {
            dto.setResourceIdentifier(catalog.getManagedResource().getResourceIdentifier());
            dto.setResourceType(catalog.getManagedResource().getResourceType().name());
            dto.setOwner(catalog.getManagedResource().getServiceOwner());
            dto.setParameterTypes(catalog.getManagedResource().getParameterTypes());
            dto.setReturnType(catalog.getManagedResource().getReturnType());
        }
        if (catalog.getFunctionGroup() != null) {
            dto.setFunctionGroupName(catalog.getFunctionGroup().getName());
        }
        return dto;
    }
}