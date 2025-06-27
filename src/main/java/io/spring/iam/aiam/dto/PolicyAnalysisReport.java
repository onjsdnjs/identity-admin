package io.spring.iam.aiam.dto;

import java.util.List;

/**
 * AI가 시스템의 전반적인 보안 상태를 분석한 리포트를 담는 DTO.
 */
public record PolicyAnalysisReport(
        String insightType, // "SOD_VIOLATION", "DORMANT_PERMISSION" 등
        String description, // 분석된 내용에 대한 한글 설명
        List<Long> relatedEntityIds, // 관련된 사용자, 역할, 정책 등의 ID
        String recommendation // 개선을 위한 제안
) {}