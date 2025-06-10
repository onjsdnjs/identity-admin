package io.spring.identityadmin.domain.dto;

import java.util.List;
import java.util.Map;

public record GrantRequestDto(
        List<SubjectDto> subjects,   // 권한을 받을 주체 목록
        List<Long> resourceIds,      // 권한을 적용할 리소스 ID 목록
        List<String> actions,        // 부여할 행위 목록 (e.g., "READ", "WRITE")
        List<ConditionDto> conditions, // 적용할 조건 목록
        String grantReason            // 권한 부여 사유 (감사 로그용)
) {
    public record SubjectDto(Long id, String type) {}
    public record ConditionDto(String type, Map<String, String> params) {}
}
