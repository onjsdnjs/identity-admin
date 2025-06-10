package io.spring.identityadmin.domain.dto;

import java.util.List;
import java.util.Map;

// 권한 현황 조회를 위한 DTO
public record EntitlementDto(
        String subjectType,   // "USER", "GROUP"
        String subjectName,   // "관리자", "개발팀"
        String resourceName,  // "사용자 상세 정보"
        List<String> actions, // ["읽기", "수정"]
        List<String> conditions, // ["업무 시간 내", "사내 IP"]
        Long policyId         // 이 권한의 근거가 되는 정책 ID
) {}

