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
 * 시스템 내부의 List<Map<String, String>> 형식과 상호 변환 지원
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
     * 배치 크기 (기본값: 5)
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
         * Map에서 ResourceItem 생성
         */
        public static ResourceItem fromMap(Map<String, String> resourceMap) {
            return ResourceItem.builder()
                    .identifier(resourceMap.get("identifier"))
                    .owner(resourceMap.get("owner"))
                    .build();
        }
    }
    
    /**
     * List<Map<String, String>>에서 변환하는 팩토리 메서드
     */
    public static ResourceNamingSuggestionRequest fromMapList(List<Map<String, String>> resourceMaps) {
        List<ResourceItem> items = resourceMaps.stream()
                .map(ResourceItem::fromMap)
                .toList();
                
        return ResourceNamingSuggestionRequest.builder()
                .resources(items)
                .build();
    }
} 