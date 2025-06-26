package io.spring.identityadmin.ai.controller;

import io.spring.identityadmin.ai.AINativeIAMSynapseArbiterFromOllama;
import io.spring.identityadmin.ai.dto.ConditionValidationRequest;
import io.spring.identityadmin.ai.dto.ConditionValidationResponse;
import io.spring.identityadmin.domain.dto.AiGeneratedPolicyDraftDto;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.ManagedResource;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.repository.ManagedResourceRepository;
import io.spring.identityadmin.resource.service.ConditionCompatibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import io.spring.identityadmin.ai.dto.PolicyGenerationRequest;
import java.util.List;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

// 기존 AiApiController를 참고하여 스트리밍 메서드를 추가하는 예시

@RestController
@RequestMapping("/api/ai/policies")
@RequiredArgsConstructor
@Slf4j
public class AiApiController {

    private final AINativeIAMSynapseArbiterFromOllama aiNativeIAMAdvisor;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final ManagedResourceRepository managedResourceRepository;
    private final ConditionCompatibilityService conditionCompatibilityService;

    /**
     * AI로 정책 초안을 스트리밍 방식으로 생성합니다.
     * Server-Sent Events (SSE) 형식으로 응답을 스트리밍합니다.
     */
    @PostMapping(value = "/generate-from-text/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generatePolicyFromTextStream(@RequestBody PolicyGenerationRequest request) {

        String naturalLanguageQuery = request.naturalLanguageQuery();
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("ERROR: naturalLanguageQuery is required")
                    .build());
        }

        log.info("🔥 AI 스트리밍 정책 생성 요청: {}", naturalLanguageQuery);
        if (request.availableItems() != null) {
            log.info("🎯 사용 가능한 항목들: 역할 {}개, 권한 {}개, 조건 {}개", 
                request.availableItems().roles() != null ? request.availableItems().roles().size() : 0,
                request.availableItems().permissions() != null ? request.availableItems().permissions().size() : 0,
                request.availableItems().conditions() != null ? request.availableItems().conditions().size() : 0);
        }

        try {
            // 사용 가능한 항목들을 AI 서비스에 전달 (임시로 기존 메서드 사용)
            return aiNativeIAMAdvisor.generatePolicyFromTextStream(naturalLanguageQuery)
                    .map(chunk -> {
                        // 청크를 SSE 형식으로 변환
                        return ServerSentEvent.<String>builder()
                                .data(chunk)
                                .build();
                    })
                    .concatWith(
                            // 스트림 종료 시그널
                            Mono.just(ServerSentEvent.<String>builder()
                                    .data("[DONE]")
                                    .build())
                    )
                    .onErrorResume(error -> {
                        log.error("🔥 스트리밍 중 오류 발생", error);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data("ERROR: " + error.getMessage())
                                .build());
                    });

        } catch (Exception e) {
            log.error("🔥 AI 스트리밍 정책 생성 실패", e);
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("ERROR: " + e.getMessage())
                    .build());
        }
    }

    /**
     * AI로 정책 초안을 일반 방식으로 생성합니다 (fallback용).
     */
    @PostMapping("/generate-from-text")
    public ResponseEntity<AiGeneratedPolicyDraftDto> generatePolicyFromText(
            @RequestBody PolicyGenerationRequest request) {

        String naturalLanguageQuery = request.naturalLanguageQuery();
        if (naturalLanguageQuery == null || naturalLanguageQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("AI 정책 생성 요청: {}", naturalLanguageQuery);
        if (request.availableItems() != null) {
            log.info("🎯 사용 가능한 항목들: 역할 {}개, 권한 {}개, 조건 {}개", 
                request.availableItems().roles() != null ? request.availableItems().roles().size() : 0,
                request.availableItems().permissions() != null ? request.availableItems().permissions().size() : 0,
                request.availableItems().conditions() != null ? request.availableItems().conditions().size() : 0);
        }

        try {
            // 사용 가능한 항목들을 AI 서비스에 전달 (임시로 기존 메서드 사용)
            AiGeneratedPolicyDraftDto result = aiNativeIAMAdvisor.generatePolicyFromTextByAi(naturalLanguageQuery);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI 정책 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/validate-condition")
    public ResponseEntity<ConditionValidationResponse> validateCondition(@RequestBody ConditionValidationRequest request) {
        ConditionValidationResponse response = aiNativeIAMAdvisor.validateCondition(
                request.resourceIdentifier(), request.conditionSpel()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * 🔄 3단계: 특정 리소스에 대한 실시간 조건 추천 API
     */
    @PostMapping("/recommend-conditions")
    public ResponseEntity<Map<String, Object>> recommendConditions(@RequestBody RecommendConditionsRequest request) {
        log.info("🎯 조건 추천 요청: 리소스={}, 컨텍스트={}", request.resourceIdentifier(), request.context());
        
        try {
            // 리소스 정보 조회
            ManagedResource resource = managedResourceRepository.findByResourceIdentifier(request.resourceIdentifier())
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + request.resourceIdentifier()));

            // 모든 조건 템플릿 조회
            List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
            
            // 호환성 검사 수행
            Map<Long, ConditionCompatibilityService.CompatibilityResult> compatibilityResults = 
                conditionCompatibilityService.checkBatchCompatibility(allConditions, resource);

            // 호환 가능한 조건들을 분류별로 그룹화
            Map<ConditionTemplate.ConditionClassification, List<RecommendedCondition>> recommendedByClass = 
                new EnumMap<>(ConditionTemplate.ConditionClassification.class);

            for (ConditionTemplate condition : allConditions) {
                ConditionCompatibilityService.CompatibilityResult result = compatibilityResults.get(condition.getId());
                if (result != null && result.isCompatible()) {
                    // 🔄 개선: 스마트 매칭 점수 계산 (권한명 정보 없을 시 기본 추천 점수 사용)
                    double matchingScore = calculateRecommendationScore(condition, request.context());
                    
                    RecommendedCondition recommendedCondition = new RecommendedCondition(
                        condition.getId(),
                        condition.getName(),
                        condition.getDescription(),
                        condition.getSpelTemplate(),
                        condition.getClassification(),
                        condition.getRiskLevel(),
                        condition.getComplexityScore(),
                        result.getReason(),
                        matchingScore
                    );
                    
                    recommendedByClass.computeIfAbsent(condition.getClassification(), 
                        k -> new ArrayList<>()).add(recommendedCondition);
                }
            }

            // 각 분류별로 추천 점수순 정렬
            recommendedByClass.values().forEach(list -> 
                list.sort((a, b) -> Double.compare(b.recommendationScore(), a.recommendationScore())));

            Map<String, Object> response = new HashMap<>();
            response.put("resourceIdentifier", request.resourceIdentifier());
            response.put("resourceFriendlyName", resource.getFriendlyName());
            response.put("recommendedConditions", recommendedByClass);
            response.put("totalRecommended", recommendedByClass.values().stream()
                .mapToInt(List::size).sum());
            response.put("statistics", calculateRecommendationStatistics(recommendedByClass));

            log.info("✅ 조건 추천 완료: {} 개 조건 추천", 
                recommendedByClass.values().stream().mapToInt(List::size).sum());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🔥 조건 추천 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "조건 추천 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 조건의 추천 점수를 계산합니다.
     */
    private double calculateRecommendationScore(ConditionTemplate condition, String context) {
        double score = 0.0;
        
        // 기본 점수 (분류별)
        switch (condition.getClassification()) {
            case UNIVERSAL -> score += 1.0;           // 범용 조건은 높은 점수
            case CONTEXT_DEPENDENT -> score += 0.7;   // 컨텍스트 의존은 중간 점수
            case CUSTOM_COMPLEX -> score += 0.4;      // 복잡한 조건은 낮은 점수
        }
        
        // 위험도에 따른 점수 조정
        if (condition.getRiskLevel() != null) {
            switch (condition.getRiskLevel()) {
                case LOW -> score += 0.3;
                case MEDIUM -> score += 0.1;
                case HIGH -> score -= 0.2;
            }
        }
        
        // 복잡도에 따른 점수 조정 (낮을수록 좋음)
        if (condition.getComplexityScore() != null) {
            score += (10 - condition.getComplexityScore()) * 0.05;
        }
        
        // 컨텍스트 기반 점수 조정
        if (context != null && !context.trim().isEmpty()) {
            String lowerContext = context.toLowerCase();
            String lowerName = condition.getName().toLowerCase();
            String lowerDesc = condition.getDescription() != null ? condition.getDescription().toLowerCase() : "";
            
            // 키워드 매칭
            if (lowerName.contains("시간") && lowerContext.contains("time")) score += 0.5;
            if (lowerName.contains("ip") && lowerContext.contains("ip")) score += 0.5;
            if (lowerName.contains("본인") && lowerContext.contains("owner")) score += 0.5;
            if (lowerDesc.contains(lowerContext) || lowerName.contains(lowerContext)) score += 0.3;
        }
        
        return Math.max(0.0, Math.min(2.0, score)); // 0.0 ~ 2.0 범위로 제한
    }

    /**
     * 🔄 개선: 권한명과 조건명 스마트 매칭 API
     */
    @PostMapping("/smart-match-conditions")
    public ResponseEntity<Map<String, Object>> smartMatchConditions(@RequestBody SmartMatchRequest request) {
        log.info("🎯 스마트 조건 매칭 요청: 권한={}, 리소스={}", request.permissionName(), request.resourceIdentifier());
        
        try {
            // 리소스 정보 조회
            ManagedResource resource = managedResourceRepository.findByResourceIdentifier(request.resourceIdentifier())
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + request.resourceIdentifier()));

            // 모든 조건 템플릿 조회
            List<ConditionTemplate> allConditions = conditionTemplateRepository.findAll();
            
            // 호환성 검사 수행
            Map<Long, ConditionCompatibilityService.CompatibilityResult> compatibilityResults = 
                conditionCompatibilityService.checkBatchCompatibility(allConditions, resource);

            // 호환 가능한 조건들에 대해 스마트 매칭 점수 계산
            List<SmartMatchedCondition> smartMatched = new ArrayList<>();

            for (ConditionTemplate condition : allConditions) {
                ConditionCompatibilityService.CompatibilityResult result = compatibilityResults.get(condition.getId());
                if (result != null && result.isCompatible()) {
                    double smartScore = calculateSmartMatchingScore(condition, request.permissionName(), request.context());
                    
                    SmartMatchedCondition matchedCondition = new SmartMatchedCondition(
                        condition.getId(),
                        condition.getName(),
                        condition.getDescription(),
                        condition.getSpelTemplate(),
                        condition.getClassification(),
                        condition.getRiskLevel(),
                        condition.getComplexityScore(),
                        result.getReason(),
                        smartScore,
                        calculateMatchingReason(condition, request.permissionName())
                    );
                    
                    smartMatched.add(matchedCondition);
                }
            }

            // 스마트 매칭 점수순으로 정렬
            smartMatched.sort((a, b) -> Double.compare(b.smartMatchingScore(), a.smartMatchingScore()));

            Map<String, Object> response = new HashMap<>();
            response.put("permissionName", request.permissionName());
            response.put("resourceIdentifier", request.resourceIdentifier());
            response.put("resourceFriendlyName", resource.getFriendlyName());
            response.put("smartMatchedConditions", smartMatched);
            response.put("totalMatched", smartMatched.size());
            response.put("highScoreConditions", smartMatched.stream()
                .filter(c -> c.smartMatchingScore() >= 3.0)
                .collect(Collectors.toList()));

            log.info("✅ 스마트 매칭 완료: {} 개 조건, 고점수: {} 개", 
                smartMatched.size(),
                smartMatched.stream().mapToLong(c -> c.smartMatchingScore() >= 3.0 ? 1 : 0).sum());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("🔥 스마트 매칭 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "스마트 매칭 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔄 개선: 권한명과 조건명 스마트 매칭 점수 계산
     */
    private double calculateSmartMatchingScore(ConditionTemplate condition, String permissionName, String context) {
        double score = calculateRecommendationScore(condition, context);
        
        if (permissionName == null || condition.getName() == null) {
            return score;
        }
        
        String lowerPermission = permissionName.toLowerCase();
        String lowerCondition = condition.getName().toLowerCase();
        
        // 🎯 핵심 개선: 권한명-조건명 의미적 매칭
        
        // 1. 완전 일치 (권한명이 조건명에 포함되거나 그 반대)
        String cleanPermission = lowerPermission.replaceAll("[^가-힣a-z0-9]", "");
        String cleanCondition = lowerCondition.replaceAll("[^가-힣a-z0-9]", "");
        
        if (cleanCondition.contains(cleanPermission) || cleanPermission.contains(cleanCondition)) {
            score += 3.0; // 높은 점수
        }
        
        // 2. 핵심 키워드 매칭
        String[] permissionWords = lowerPermission.split("\\s+");
        String[] conditionWords = lowerCondition.split("\\s+");
        
        int matchedWords = 0;
        for (String pWord : permissionWords) {
            if (pWord.length() > 1) { // 한 글자 단어는 제외
                for (String cWord : conditionWords) {
                    if (cWord.contains(pWord) || pWord.contains(cWord)) {
                        matchedWords++;
                        break;
                    }
                }
            }
        }
        
        if (matchedWords > 0) {
            score += (double) matchedWords / permissionWords.length * 2.0;
        }
        
        // 3. 엔티티 타입 매칭 (사용자 ↔ User)
        if (containsEntity(lowerPermission, "사용자") && containsEntity(lowerCondition, "사용자")) score += 1.0;
        if (containsEntity(lowerPermission, "문서") && containsEntity(lowerCondition, "문서")) score += 1.0;
        if (containsEntity(lowerPermission, "그룹") && containsEntity(lowerCondition, "그룹")) score += 1.0;
        if (containsEntity(lowerPermission, "권한") && containsEntity(lowerCondition, "권한")) score += 1.0;
        if (containsEntity(lowerPermission, "역할") && containsEntity(lowerCondition, "역할")) score += 1.0;
        if (containsEntity(lowerPermission, "정책") && containsEntity(lowerCondition, "정책")) score += 1.0;
        
        // 4. 액션 타입 매칭 (수정 ↔ 수정, 삭제 ↔ 삭제)
        if (containsAction(lowerPermission, "수정") && containsAction(lowerCondition, "수정")) score += 1.5;
        if (containsAction(lowerPermission, "삭제") && containsAction(lowerCondition, "삭제")) score += 1.5;
        if (containsAction(lowerPermission, "조회") && containsAction(lowerCondition, "조회")) score += 1.5;
        if (containsAction(lowerPermission, "생성") && containsAction(lowerCondition, "생성")) score += 1.5;
        if (containsAction(lowerPermission, "관리") && containsAction(lowerCondition, "관리")) score += 1.5;
        
        // 5. 특수 패턴 매칭
        if (lowerPermission.contains("본인") && lowerCondition.contains("본인")) score += 2.0;
        if (lowerPermission.contains("소유자") && lowerCondition.contains("소유자")) score += 2.0;
        if (lowerPermission.contains("관리자") && lowerCondition.contains("관리자")) score += 1.5;
        
        return Math.max(0.0, Math.min(5.0, score)); // 확장된 범위로 제한
    }
    
    /**
     * 매칭 이유 계산
     */
    private String calculateMatchingReason(ConditionTemplate condition, String permissionName) {
        if (permissionName == null || condition.getName() == null) {
            return "기본 추천";
        }
        
        List<String> reasons = new ArrayList<>();
        String lowerPermission = permissionName.toLowerCase();
        String lowerCondition = condition.getName().toLowerCase();
        
        // 엔티티 매칭
        if (containsEntity(lowerPermission, "사용자") && containsEntity(lowerCondition, "사용자")) {
            reasons.add("사용자 엔티티 매칭");
        }
        if (containsEntity(lowerPermission, "문서") && containsEntity(lowerCondition, "문서")) {
            reasons.add("문서 엔티티 매칭");
        }
        
        // 액션 매칭
        if (containsAction(lowerPermission, "수정") && containsAction(lowerCondition, "수정")) {
            reasons.add("수정 액션 매칭");
        }
        if (containsAction(lowerPermission, "삭제") && containsAction(lowerCondition, "삭제")) {
            reasons.add("삭제 액션 매칭");
        }
        if (containsAction(lowerPermission, "조회") && containsAction(lowerCondition, "조회")) {
            reasons.add("조회 액션 매칭");
        }
        
        // 특수 패턴
        if (lowerPermission.contains("본인") && lowerCondition.contains("본인")) {
            reasons.add("본인 확인 패턴");
        }
        
        return reasons.isEmpty() ? "일반 호환성" : String.join(", ", reasons);
    }
    
    /**
     * 엔티티 타입 포함 여부 확인
     */
    private boolean containsEntity(String text, String entity) {
        return text.contains(entity) || 
               (entity.equals("사용자") && (text.contains("user") || text.contains("회원"))) ||
               (entity.equals("문서") && (text.contains("document") || text.contains("파일") || text.contains("file"))) ||
               (entity.equals("그룹") && (text.contains("group") || text.contains("팀")));
    }
    
    /**
     * 액션 타입 포함 여부 확인
     */
    private boolean containsAction(String text, String action) {
        switch (action) {
            case "수정":
                return text.contains("수정") || text.contains("edit") || text.contains("update") || text.contains("modify");
            case "삭제":
                return text.contains("삭제") || text.contains("delete") || text.contains("remove");
            case "조회":
                return text.contains("조회") || text.contains("read") || text.contains("view") || text.contains("get") || text.contains("find");
            case "생성":
                return text.contains("생성") || text.contains("create") || text.contains("add") || text.contains("insert");
            default:
                return text.contains(action);
        }
    }

    /**
     * 추천 통계를 계산합니다.
     */
    private Map<String, Object> calculateRecommendationStatistics(
            Map<ConditionTemplate.ConditionClassification, List<RecommendedCondition>> recommendedByClass) {
        
        Map<String, Object> stats = new HashMap<>();
        
        int totalCount = recommendedByClass.values().stream().mapToInt(List::size).sum();
        stats.put("totalRecommended", totalCount);
        
        Map<String, Integer> countByClass = new HashMap<>();
        for (Map.Entry<ConditionTemplate.ConditionClassification, List<RecommendedCondition>> entry : recommendedByClass.entrySet()) {
            countByClass.put(entry.getKey().name(), entry.getValue().size());
        }
        stats.put("countByClassification", countByClass);
        
        // 평균 추천 점수
        double avgScore = recommendedByClass.values().stream()
            .flatMap(List::stream)
            .mapToDouble(RecommendedCondition::recommendationScore)
            .average()
            .orElse(0.0);
        stats.put("averageRecommendationScore", Math.round(avgScore * 100.0) / 100.0);
        
        return stats;
    }

    /**
     * 조건 추천 요청 DTO
     */
    public record RecommendConditionsRequest(
        String resourceIdentifier,
        String context  // 추가 컨텍스트 (예: "time-based", "ip-restriction" 등)
    ) {}

    /**
     * 추천된 조건 정보 DTO
     */
    public record RecommendedCondition(
        Long id,
        String name,
        String description,
        String spelTemplate,
        ConditionTemplate.ConditionClassification classification,
        ConditionTemplate.RiskLevel riskLevel,
        Integer complexityScore,
        String compatibilityReason,
        double recommendationScore
    ) {}

    /**
     * 🔄 스마트 매칭 요청 DTO
     */
    public record SmartMatchRequest(
        String permissionName,
        String resourceIdentifier, 
        String context
    ) {}

    /**
     * 🔄 스마트 매칭된 조건 정보 DTO
     */
    public record SmartMatchedCondition(
        Long id,
        String name,
        String description,
        String spelTemplate,
        ConditionTemplate.ConditionClassification classification,
        ConditionTemplate.RiskLevel riskLevel,
        Integer complexityScore,
        String compatibilityReason,
        double smartMatchingScore,
        String matchingReason
    ) {}
}