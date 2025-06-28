package io.spring.iam.aiam.protocol.response;

import io.spring.iam.aiam.dto.ResourceNameSuggestion;
import io.spring.iam.aiam.protocol.AIAMResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 리소스 네이밍 AI 진단 응답 DTO
 * 시스템 내부의 Map<String, ResourceNameSuggestion> 형식과 상호 변환 지원
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceNamingSuggestionResponse implements AIAMResponse {

    /**
     * 성공적으로 처리된 리소스 네이밍 제안들
     */
    private List<ResourceNamingSuggestion> suggestions;
    
    /**
     * 처리 실패한 리소스 식별자들
     */
    private List<String> failedIdentifiers;
    
    /**
     * 전체 처리 통계
     */
    private ProcessingStats stats;
    
    /**
     * 개별 리소스 네이밍 제안
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceNamingSuggestion {
        /**
         * 원본 기술적 식별자
         */
        private String identifier;
        
        /**
         * AI가 제안한 친화적 이름
         */
        private String friendlyName;
        
        /**
         * AI가 제안한 상세 설명
         */
        private String description;
        
        /**
         * AI 신뢰도 점수 (0.0 ~ 1.0)
         */
        private double confidence;
        
        /**
         * ResourceNameSuggestion으로 변환
         */
        public ResourceNameSuggestion toResourceNameSuggestion() {
            return new ResourceNameSuggestion(friendlyName, description);
        }
        
        /**
         * ResourceNameSuggestion에서 변환
         */
        public static ResourceNamingSuggestion fromResourceNameSuggestion(String identifier, ResourceNameSuggestion suggestion) {
            return ResourceNamingSuggestion.builder()
                    .identifier(identifier)
                    .friendlyName(suggestion.friendlyName())
                    .description(suggestion.description())
                    .confidence(0.8) // 기본 신뢰도
                    .build();
        }
    }
    
    /**
     * 처리 통계
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingStats {
        private int totalRequested;
        private int successfullyProcessed;
        private int failed;
        private long processingTimeMs;
        
        public double getSuccessRate() {
            return totalRequested > 0 ? (double) successfullyProcessed / totalRequested : 0.0;
        }
    }
    
    /**
     * Map<String, ResourceNameSuggestion> 형식으로 변환
     */
    public Map<String, ResourceNameSuggestion> toResourceNameSuggestionMap() {
        return suggestions.stream()
                .collect(Collectors.toMap(
                        ResourceNamingSuggestion::getIdentifier,
                        ResourceNamingSuggestion::toResourceNameSuggestion
                ));
    }
    
    /**
     * Map<String, ResourceNameSuggestion>에서 변환하는 팩토리 메서드
     */
    public static ResourceNamingSuggestionResponse fromResourceNameSuggestionMap(Map<String, ResourceNameSuggestion> suggestionMap) {
        List<ResourceNamingSuggestion> suggestions = suggestionMap.entrySet().stream()
                .map(entry -> ResourceNamingSuggestion.fromResourceNameSuggestion(entry.getKey(), entry.getValue()))
                .toList();
                
        ProcessingStats stats = ProcessingStats.builder()
                .totalRequested(suggestionMap.size())
                .successfullyProcessed(suggestionMap.size())
                .failed(0)
                .build();
                
        return ResourceNamingSuggestionResponse.builder()
                .suggestions(suggestions)
                .failedIdentifiers(List.of())
                .stats(stats)
                .build();
    }
} 