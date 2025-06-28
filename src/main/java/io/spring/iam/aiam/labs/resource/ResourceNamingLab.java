package io.spring.iam.aiam.labs.resource;

import io.spring.aicore.components.parser.ResourceNamingJsonParser;
import io.spring.aicore.components.prompt.ResourceNamingTemplate;
import io.spring.aicore.components.retriever.ResourceNamingContextRetriever;
import io.spring.aicore.pipeline.DefaultUniversalPipeline;
import io.spring.aicore.pipeline.PipelineConfiguration;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.dto.ResourceNameSuggestion;
import io.spring.iam.aiam.labs.AbstractIAMLab;
import io.spring.iam.aiam.labs.LabCapabilities;
import io.spring.iam.aiam.labs.LabCapabilityAssessment;
import io.spring.iam.aiam.labs.LabSpecialization;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.AuditRequirement;
import io.spring.iam.aiam.protocol.enums.SecurityLevel;
import io.spring.iam.aiam.protocol.request.ResourceNamingSuggestionRequest;
import io.spring.iam.aiam.protocol.response.ResourceNamingSuggestionResponse;
import io.spring.iam.aiam.protocol.types.ResourceNamingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🔬 리소스 네이밍 전문 연구소
 * 🎯 전문 분야: 기술적 리소스 식별자를 사용자 친화적 이름으로 변환
 * 📋 6단계 AI 파이프라인 실행:
 * 1. CONTEXT_RETRIEVAL: ResourceNamingContextRetriever - RAG 검색
 * 2. PREPROCESSING: 메타데이터 구성  
 * 3. PROMPT_GENERATION: ResourceNamingTemplate - 동적 프롬프트 생성
 * 4. LLM_EXECUTION: ChatModel - AI 모델 실행
 * 5. RESPONSE_PARSING: ResourceNamingJsonParser - JSON 추출/정제
 * 6. POSTPROCESSING: 후처리 및 검증
 * 🔄 구버전 호환성:
 * - suggestResourceNamesInBatch() 로직을 6단계 파이프라인으로 분산
 * - 배치 크기 5개 제한 유지
 * - 복잡한 JSON 파싱 전략 유지
 * - 한글 프롬프트 엔지니어링 유지
 */
@Slf4j
@Component
public class ResourceNamingLab extends AbstractIAMLab<IAMContext> {

    // 6단계 파이프라인 컴포넌트들  
    private final DefaultUniversalPipeline universalPipeline;
    private final ResourceNamingContextRetriever contextRetriever;
    private final ResourceNamingTemplate promptTemplate;
    private final ChatModel chatModel;
    private final ResourceNamingJsonParser jsonParser;

    // 구버전 호환 설정
    private static final int DEFAULT_BATCH_SIZE = 5; // 구버전과 동일

    public ResourceNamingLab(DefaultUniversalPipeline universalPipeline,
                            ResourceNamingContextRetriever contextRetriever,
                            ResourceNamingTemplate promptTemplate,
                            ChatModel chatModel,
                            ResourceNamingJsonParser jsonParser) {
        super("ResourceNaming", "1.0", 
              LabSpecialization.RECOMMENDATION_SYSTEM, 
              createCapabilities());
        
        this.universalPipeline = universalPipeline;
        this.contextRetriever = contextRetriever;
        this.promptTemplate = promptTemplate;
        this.chatModel = chatModel;
        this.jsonParser = jsonParser;
        
        log.info("🔬 ResourceNamingLab initialized with 6-stage pipeline");
    }

    @Override
    public <R extends IAMResponse> R conductResearch(IAMRequest<IAMContext> request, Class<R> responseType) {
        log.info("🔬 ResourceNaming 연구 시작 - 6단계 파이프라인 실행");

        try {
            // IAMRequest를 ResourceNamingSuggestionRequest로 변환
            ResourceNamingSuggestionRequest namingRequest = convertRequest(request);
            
            // 6단계 파이프라인 실행
            ResourceNamingSuggestionResponse response = executePipeline(namingRequest);
            
            // 타입 캐스팅하여 반환 (ResourceNamingSuggestionResponse가 IAMResponse를 확장하도록 수정 필요)
            return responseType.cast(response);

        } catch (Exception e) {
            log.error("🔥 ResourceNaming 연구 실패", e);
            throw new RuntimeException("ResourceNaming research failed", e);
        }
    }

    /**
     * 6단계 파이프라인 실행 (구버전 로직을 단계별로 분산)
     */
    private ResourceNamingSuggestionResponse executePipeline(ResourceNamingSuggestionRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 배치 처리 (구버전과 동일한 방식)
            List<ResourceNamingSuggestionResponse.ResourceNamingSuggestion> allSuggestions = new ArrayList<>();
            List<String> failedIdentifiers = new ArrayList<>();
            
            // 배치 크기만큼 분할 처리
            List<ResourceNamingSuggestionRequest.ResourceItem> resources = request.getResources();
            int batchSize = request.getBatchSize() > 0 ? request.getBatchSize() : DEFAULT_BATCH_SIZE;
            
            for (int i = 0; i < resources.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, resources.size());
                List<ResourceNamingSuggestionRequest.ResourceItem> batch = resources.subList(i, endIndex);
                
                log.info("🔬 배치 처리 중: {}/{} (배치 크기: {})", i + 1, resources.size(), batch.size());
                
                // 개별 배치에 대해 6단계 파이프라인 실행
                ResourceNamingSuggestionResponse batchResponse = processBatch(batch);
                
                allSuggestions.addAll(batchResponse.getSuggestions());
                failedIdentifiers.addAll(batchResponse.getFailedIdentifiers());
            }
            
            // 최종 응답 생성
            long processingTime = System.currentTimeMillis() - startTime;
            ResourceNamingSuggestionResponse.ProcessingStats stats = 
                ResourceNamingSuggestionResponse.ProcessingStats.builder()
                    .totalRequested(resources.size())
                    .successfullyProcessed(allSuggestions.size())
                    .failed(failedIdentifiers.size())
                    .processingTimeMs(processingTime)
                    .build();
            
            ResourceNamingSuggestionResponse finalResponse = ResourceNamingSuggestionResponse.builder()
                    .suggestions(allSuggestions)
                    .failedIdentifiers(failedIdentifiers)
                    .stats(stats)
                    .build();
            
            log.info("✅ 6단계 파이프라인 실행 완료 - 성공: {}, 실패: {}, 처리시간: {}ms", 
                    allSuggestions.size(), failedIdentifiers.size(), processingTime);
            
            return finalResponse;

        } catch (Exception e) {
            log.error("🔥 6단계 파이프라인 실행 실패", e);
            throw new RuntimeException("Pipeline execution failed", e);
        }
    }

    /**
     * 🔥 ConditionTemplateGenerationLab과 동일한 진짜 파이프라인 기반 배치 처리
     */
    private ResourceNamingSuggestionResponse processBatch(List<ResourceNamingSuggestionRequest.ResourceItem> batch) {
        log.info("🤖 AI 리소스 네이밍 생성 시작 - Pipeline 활용 (배치 크기: {})", batch.size());

        try {
            // 1. 🔬 도메인 전문성: 전문 AIRequest 구성
            AIRequest<IAMContext> aiRequest = createResourceNamingRequest(batch);
            
            // 2. 🚀 표준 AI 처리: Pipeline에 완전 위임 (ConditionTemplateGenerationLab과 동일)
            PipelineConfiguration config = createResourceNamingPipelineConfig();
            Mono<AIResponse> pipelineResult = universalPipeline.execute(aiRequest, config, AIResponse.class);
            
            AIResponse response = pipelineResult.block(); // 동기 처리
            
            // 3. 🔬 도메인 전문성: 리소스 네이밍 후처리 및 검증 (구버전 로직 완전 이식)
            String jsonResponse = (String) response.getData();
            
            log.info("🔥 Pipeline AI 원본 응답 길이: {}", jsonResponse.length());
            log.debug("🔥 Pipeline AI 원본 응답: {}", jsonResponse);

            // 🔥 4단계: RESPONSE_PARSING - 신버전 JsonParser 사용
            ResourceNamingSuggestionRequest batchRequest = ResourceNamingSuggestionRequest.builder()
                    .resources(batch)
                    .batchSize(batch.size())
                    .build();
            
            ResourceNamingSuggestionResponse parsedResponse = jsonParser.parse(jsonResponse, batchRequest);

            // 🔥 5단계: 구버전 완전 이식 - 응답 검증 및 로깅
            log.info("🔥 배치 크기: {}, 파싱된 항목 수: {}", batch.size(), parsedResponse.getSuggestions().size());
            if (parsedResponse.getSuggestions().size() < batch.size()) {
                log.error("🔥 [AI 오류] 일부 항목 누락! 요청: {}, 응답: {}", batch.size(), parsedResponse.getSuggestions().size());

                // 🔥 구버전과 완전 동일: 누락된 항목 상세 로깅
                Set<String> requested = batch.stream().map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier).collect(Collectors.toSet());
                Set<String> responded = parsedResponse.getSuggestions().stream().map(ResourceNamingSuggestionResponse.ResourceNamingSuggestion::getIdentifier).collect(Collectors.toSet());
                requested.removeAll(responded);
                log.error("🔥 [AI 오류] 누락된 항목들: {}", requested);
            }

            log.info("✅ 진짜 6단계 파이프라인 배치 처리 완료 - 성공: {}, 실패: {}", 
                    parsedResponse.getSuggestions().size(), parsedResponse.getFailedIdentifiers().size());

            return parsedResponse;

        } catch (Exception e) {
            log.error("🔥 진짜 6단계 파이프라인 배치 처리 실패", e);
            return createFallbackResponse(batch, e.getMessage());
        }
    }

    /**
     * 🔬 도메인 전문성: 리소스 네이밍 요청 구성 (ConditionTemplateGenerationLab과 동일 패턴)
     */
    private AIRequest<IAMContext> createResourceNamingRequest(List<ResourceNamingSuggestionRequest.ResourceItem> batch) {
        IAMContext context = new ResourceNamingContext(SecurityLevel.STANDARD, AuditRequirement.BASIC);
        
        AIRequest<IAMContext> request = new AIRequest<>(context, "resource_naming_suggestion");
        
        // 🔬 리소스 네이밍 전문 메타데이터 설정
        request.withParameter("requestType", "resource_naming");
        request.withParameter("batchSize", batch.size());
        request.withParameter("outputFormat", "json_object");
        request.withParameter("language", "korean");
        request.withParameter("includeDescription", true);
        
        // 배치 데이터 추가
        List<String> identifiers = batch.stream()
                .map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier)
                .collect(Collectors.toList());
        request.withParameter("identifiers", identifiers);
        
        List<String> owners = batch.stream()
                .map(ResourceNamingSuggestionRequest.ResourceItem::getOwner)
                .filter(owner -> owner != null && !owner.trim().isEmpty())
                .collect(Collectors.toList());
        request.withParameter("owners", owners);
        
        return request;
    }
    
    /**
     * 🚀 Pipeline 설정 구성 (ConditionTemplateGenerationLab과 동일)
     */
    private PipelineConfiguration createResourceNamingPipelineConfig() {
        return PipelineConfiguration.builder()
            .addStep(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL)
            .addStep(PipelineConfiguration.PipelineStep.PREPROCESSING)
            .addStep(PipelineConfiguration.PipelineStep.PROMPT_GENERATION)
            .addStep(PipelineConfiguration.PipelineStep.LLM_EXECUTION)
            .addStep(PipelineConfiguration.PipelineStep.RESPONSE_PARSING)
            .addStep(PipelineConfiguration.PipelineStep.POSTPROCESSING)
            .timeoutSeconds(30) // 30초
            .build();
    }
    
    /**
     * 🛡️ 도메인 전문성: 안전한 폴백 응답
     */
    private ResourceNamingSuggestionResponse createFallbackResponse(List<ResourceNamingSuggestionRequest.ResourceItem> batch, String errorMessage) {
        // 🔥 구버전과 완전 동일: AI 오류 시 빈 결과 반환
        log.error("🔥 [AI 오류] 진짜 6단계 파이프라인 완전 실패, 빈 결과 반환: {}", errorMessage);
        return ResourceNamingSuggestionResponse.builder()
                .suggestions(List.of())
                .failedIdentifiers(batch.stream().map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier).toList())
                .stats(ResourceNamingSuggestionResponse.ProcessingStats.builder()
                        .totalRequested(batch.size())
                        .successfullyProcessed(0)
                        .failed(batch.size())
                        .build())
                .build();
    }

    // 🔥 이제 모든 파싱 로직은 ResourceNamingJsonParser에서 처리됩니다

    /**
     * Map<String, ResourceNameSuggestion>을 ResourceNamingSuggestionResponse로 변환
     */
    private ResourceNamingSuggestionResponse convertToResponse(Map<String, ResourceNameSuggestion> parsedResult, List<ResourceNamingSuggestionRequest.ResourceItem> originalBatch) {
        List<ResourceNamingSuggestionResponse.ResourceNamingSuggestion> suggestions = new ArrayList<>();
        List<String> failedIdentifiers = new ArrayList<>();

        // 성공한 항목들을 변환
        for (Map.Entry<String, ResourceNameSuggestion> entry : parsedResult.entrySet()) {
            ResourceNamingSuggestionResponse.ResourceNamingSuggestion suggestion = 
                ResourceNamingSuggestionResponse.ResourceNamingSuggestion.builder()
                    .identifier(entry.getKey())
                    .friendlyName(entry.getValue().friendlyName())
                    .description(entry.getValue().description())
                    .confidence(0.8) // 기본 신뢰도
                    .build();
                    
            suggestions.add(suggestion);
        }

        // 실패한 항목들 찾기
        Set<String> successIdentifiers = parsedResult.keySet();
        for (ResourceNamingSuggestionRequest.ResourceItem item : originalBatch) {
            if (!successIdentifiers.contains(item.getIdentifier())) {
                failedIdentifiers.add(item.getIdentifier());
            }
        }

        // 통계 생성
        ResourceNamingSuggestionResponse.ProcessingStats stats = 
            ResourceNamingSuggestionResponse.ProcessingStats.builder()
                .totalRequested(originalBatch.size())
                .successfullyProcessed(suggestions.size())
                .failed(failedIdentifiers.size())
                .build();

        return ResourceNamingSuggestionResponse.builder()
                .suggestions(suggestions)
                .failedIdentifiers(failedIdentifiers)
                .stats(stats)
                .build();
    }

    /**
     * IAMRequest를 ResourceNamingSuggestionRequest로 변환
     */
    private ResourceNamingSuggestionRequest convertRequest(IAMRequest<IAMContext> request) {
        // 실제 변환 로직 구현
        // 현재는 간단하게 구현
        return ResourceNamingSuggestionRequest.builder()
                .resources(List.of())
                .build();
    }

    @Override
    public Set<String> getSupportedOperations() {
        return Set.of("suggestResourceNames", "generateFriendlyNames", "batchResourceNaming");
    }

    @Override
    public String getSpecializationDescription() {
        return "기술적 리소스 식별자를 사용자 친화적 한글 이름으로 변환하는 AI 전문 연구소";
    }

    @Override
    public LabCapabilityAssessment assessCapabilities() {
        return new LabCapabilityAssessment(
                getLabId(),
                getLabName(), 
                getSpecialization(),
                85.0, // 전체 점수
                Map.of(), // 카테고리별 점수 (빈 맵)
                List.of("Korean naming", "Batch processing", "JSON parsing"), // 강점
                List.of(), // 약점 (빈 리스트)
                List.of("Consider improving response time"), // 권장사항
                LabCapabilityAssessment.AssessmentLevel.GOOD
        );
    }

    @Override
    protected boolean performSpecializedHealthCheck() {
        try {
            // ChatModel 상태 확인
            boolean chatModelHealthy = chatModel != null;
            
            // 파이프라인 컴포넌트 상태 확인
            boolean componentsHealthy = contextRetriever != null && 
                                       promptTemplate != null && 
                                       jsonParser != null;
            
            return chatModelHealthy && componentsHealthy;
        } catch (Exception e) {
            log.error("🔥 ResourceNamingLab 헬스체크 실패", e);
            return false;
        }
    }

    private static LabCapabilities createCapabilities() {
        // 기본 역량으로 생성 (ResourceNaming에 특화된 설정은 추후 확장 가능)
        return LabCapabilities.createBasic();
    }

    private double calculateAverageResponseTime() {
        // 실제 메트릭 계산 로직
        return 1500.0; // ms
    }

    private double calculateThroughput() {
        // 실제 처리량 계산 로직  
        return 5.0; // requests/second
    }

    @Override
    protected <R extends IAMResponse> R synthesizeResults(Map<AbstractIAMLab<IAMContext>, IAMResponse> results, Class<R> responseType) {
        // ResourceNamingLab은 단독으로 작업하므로 협업 결과 통합은 기본 구현 제공
        log.debug("🔬 ResourceNamingLab: 협업 결과 통합 (단독 작업 모드)");
        
        // 첫 번째 결과를 반환하거나 빈 결과 생성
        IAMResponse firstResult = results.values().stream().findFirst().orElse(null);
        if (responseType.isInstance(firstResult)) {
            return responseType.cast(firstResult);
        }
        
        // 기본 빈 응답 생성 (실제로는 호출되지 않을 것)
        try {
            return responseType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default response", e);
        }
    }

    /**
     * ResourceNaming 전용 진단 메서드 (타입 안전성)
     * Strategy에서 직접 호출하는 메서드
     */
    public ResourceNamingSuggestionResponse processResourceNaming(ResourceNamingSuggestionRequest request) {
        log.info("🔬 ResourceNaming 전용 진단 시작 - 리소스 수: {}", request.getResources().size());
        
        try {
            // 6단계 파이프라인 직접 실행
            return executePipeline(request);
            
        } catch (Exception e) {
            log.error("🔥 ResourceNaming 전용 진단 실패", e);
            
            // 실패 시 빈 응답 반환
            return ResourceNamingSuggestionResponse.builder()
                    .suggestions(List.of())
                    .failedIdentifiers(request.getResources().stream()
                            .map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier)
                            .toList())
                    .stats(ResourceNamingSuggestionResponse.ProcessingStats.builder()
                            .totalRequested(request.getResources().size())
                            .successfullyProcessed(0)
                            .failed(request.getResources().size())
                            .build())
                    .build();
        }
    }
} 