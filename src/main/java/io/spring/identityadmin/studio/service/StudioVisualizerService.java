package io.spring.identityadmin.studio.service;

import io.spring.identityadmin.studio.dto.*;

import java.util.List;

/**
 * Authorization Studio의 'Canvas' 패널을 위한 시각화 데이터를 생성하고,
 * 복잡한 인가 관계를 분석하여 사용자가 쉽게 이해할 수 있도록 가공합니다.
 */
public interface StudioVisualizerService {

    /**
     * "왜 이 사용자가 이 리소스에 접근할 수 있는가/없는가?"에 대한 답을 제공합니다.
     * 특정 주체와 리소스 간의 접근 경로를 추적하여 시각적인 그래프 데이터로 반환합니다.
     * @param subjectId 분석할 주체(사용자/그룹)의 ID
     * @return 접근 경로의 각 단계를 담은 DTO
     */
    AccessPathDto analyzeAccessPath(Long subjectId, String subjectType, Long permissionId);

    /**
     * 특정 주체가 보유한 모든 유효 권한 목록을 반환합니다.
     * 각 권한이 어떤 역할, 그룹, 정책으로부터 부여되었는지 근거를 함께 제공합니다.
     * @param subjectId 분석할 주체(사용자/그룹)의 ID
     * @return 유효 권한 및 그 근거를 담은 DTO 목록
     */
    List<EffectivePermissionDto> getEffectivePermissionsForSubject(Long subjectId, String subjectType);

}