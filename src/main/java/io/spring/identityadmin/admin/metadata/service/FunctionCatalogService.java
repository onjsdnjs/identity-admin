package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.domain.entity.FunctionCatalog;
import io.spring.identityadmin.domain.entity.FunctionGroup;
import io.spring.identityadmin.repository.FunctionCatalogRepository;
import io.spring.identityadmin.repository.FunctionGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCatalogService {

    private final FunctionCatalogRepository functionCatalogRepository;
    private final FunctionGroupRepository functionGroupRepository;

    /**
     * Phase 1: 개발자가 검토해야 할 '미확인' 상태의 모든 기능을 조회합니다.
     * @return List of FunctionCatalog in UNCONFIRMED state.
     */
    @Transactional
    public List<FunctionCatalog> findUnconfirmedFunctions() {
        return functionCatalogRepository.findUnconfirmedFunctions();
    }

    /**
     * Phase 1: 기능 그룹 선택 드롭다운을 채우기 위해 모든 기능 그룹을 조회합니다.
     * @return List of all FunctionGroup.
     */
    @Transactional
    public List<FunctionGroup> getAllFunctionGroups() {
        // 간단한 예시로, 초기 데이터가 없다면 기본 그룹을 생성해줍니다.
        if (functionGroupRepository.count() == 0) {
            functionGroupRepository.save(FunctionGroup.builder().name("일반").build());
            functionGroupRepository.save(FunctionGroup.builder().name("사용자 관리").build());
            functionGroupRepository.save(FunctionGroup.builder().name("정책 관리").build());
        }
        return functionGroupRepository.findAll();
    }

    /**
     * Phase 1: 개발자가 '미확인 기능'을 '활성' 상태로 변경하고 기능 그룹을 할당합니다.
     * @param catalogId 확인할 기능 카탈로그의 ID
     * @param groupId 할당할 기능 그룹의 ID
     */
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
}
