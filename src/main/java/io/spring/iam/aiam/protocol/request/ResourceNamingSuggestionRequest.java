package io.spring.iam.aiam.protocol.request;

import io.spring.iam.aiam.protocol.AIAMRequest;
import io.spring.iam.aiam.protocol.enums.RequestPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 리소스 네이밍 AI 진단 요청 DTO
 * 구버전의 List<Map<String, String>> 형식을 대체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceNamingSuggestionRequest implements AIAMRequest {
    
    /**
     * 배치로 처리할 리소스 목록
     */
    private List<ResourceItem> resources;
    
    /**
     * 배치 크기 (기본값: 5, 구버전과 동일)
     */
    @Builder.Default
    private int batchSize = 5;
    
    /**
     * 요청 우선순위
     */
    @Builder.Default
    private RequestPriority priority = RequestPriority.NORMAL;
    
    /**
     * 개별 리소스 항목
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceItem {
        /**
         * 기술적 식별자 (예: /admin/users, updateUser() 등)
         */
        private String identifier;
        
        /**
         * 서비스 소유자/팀명
         */
        private String owner;
        
        /**
         * 추가 컨텍스트 정보
         */
        private Map<String, String> metadata;
        
        /**
         * 구버전 호환성을 위한 팩토리 메서드
         */
        public static ResourceItem fromLegacyMap(Map<String, String> legacyMap) {
            return ResourceItem.builder()
                    .identifier(legacyMap.get("identifier"))
                    .owner(legacyMap.get("owner"))
                    .build();
        }
    }
    
    /**
     * 구버전 호환성을 위한 팩토리 메서드
     */
    public static ResourceNamingSuggestionRequest fromLegacyFormat(List<Map<String, String>> legacyResources) {
        List<ResourceItem> items = legacyResources.stream()
                .map(ResourceItem::fromLegacyMap)
                .toList();
                
        return ResourceNamingSuggestionRequest.builder()
                .resources(items)
                .build();
    }
} 