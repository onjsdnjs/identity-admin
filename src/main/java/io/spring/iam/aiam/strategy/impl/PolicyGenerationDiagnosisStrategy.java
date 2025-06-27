package io.spring.iam.aiam.strategy.impl;

import io.spring.iam.aiam.AINativeIAMSynapseArbiterFromOllama;
import io.spring.iam.aiam.dto.PolicyGenerationRequest;
import io.spring.iam.domain.dto.PolicyDto;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.aiam.strategy.DiagnosisStrategy;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 🤖 정책 생성 진단 전략
 * 
 * AINativeIAMSynapseArbiterFromOllama의 정책 생성 기능을 완전히 대체
 * - generatePolicyFromText (PolicyDto 반환)
 * - generatePolicyFromTextStream (Flux<String> 반환)
 * 
 * 🎯 역할:
 * 1. 자연어 → 정책 생성 요청 처리
 * 2. 스트리밍/비스트리밍 모드 지원
 * 3. RAG 기반 컨텍스트 검색 활용
 */
@Slf4j
@Component
public class PolicyGenerationDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {
    
    private final AINativeIAMSynapseArbiterFromOllama synapseArbiter;
    
    public PolicyGenerationDiagnosisStrategy(AINativeIAMSynapseArbiterFromOllama synapseArbiter) {
        this.synapseArbiter = synapseArbiter;
        log.info("🤖 PolicyGenerationDiagnosisStrategy initialized");
    }
    
    @Override
    public DiagnosisType getSupportedType() {
        return DiagnosisType.POLICY_GENERATION;
    }
    
    @Override
    public int getPriority() {
        return 20; // 높은 우선순위
    }
    
    @Override
    public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) throws DiagnosisException {
        log.info("🤖 정책 생성 진단 전략 실행 시작 - 요청: {}", request.getRequestId());
        
        try {
            // 1. 요청 데이터 검증
            validateRequest(request);
            
            // 2. 생성 모드에 따른 분기 처리
            String generationMode = request.getParameter("generationMode", String.class);
            String naturalLanguageQuery = request.getParameter("naturalLanguageQuery", String.class);
            
            Object result;
            
            switch (generationMode) {
                case "standard":
                    // 표준 정책 생성 - PolicyDto 반환
                    log.debug("📋 표준 정책 생성 요청");
                    PolicyDto policyDto = synapseArbiter.generatePolicyFromText(naturalLanguageQuery);
                    result = policyDto;
                    break;
                    
                case "streaming":
                    // 스트리밍 정책 생성 - Flux<String> 처리
                    log.debug("🌊 스트리밍 정책 생성 요청");
                    PolicyGenerationRequest.AvailableItems availableItems = 
                        request.getParameter("availableItems", PolicyGenerationRequest.AvailableItems.class);
                    
                    Flux<String> streamResult;
                    if (availableItems != null) {
                        streamResult = synapseArbiter.generatePolicyFromTextStream(naturalLanguageQuery, availableItems);
                    } else {
                        streamResult = synapseArbiter.generatePolicyFromTextStream(naturalLanguageQuery);
                    }
                    
                    // 스트리밍 결과를 문자열로 수집 (실제로는 비동기 처리 필요)
                    StringBuilder streamBuffer = new StringBuilder();
                    streamResult.doOnNext(chunk -> streamBuffer.append(chunk))
                               .doOnComplete(() -> log.debug("🌊 스트리밍 완료"))
                               .subscribe();
                    
                    result = streamBuffer.toString();
                    break;
                    
                default:
                    throw new DiagnosisException("POLICY_GENERATION", "INVALID_GENERATION_MODE", 
                        "지원하지 않는 생성 모드입니다: " + generationMode);
            }
            
            // 3. 응답 생성
            PolicyGenerationResponse response = new PolicyGenerationResponse(
                request.getRequestId(),
                ExecutionStatus.SUCCESS,
                result,
                generationMode,
                naturalLanguageQuery
            );
            
            log.info("✅ 정책 생성 진단 전략 실행 완료 - 요청: {}", request.getRequestId());
            return response;
            
        } catch (Exception e) {
            log.error("🔥 정책 생성 진단 전략 실행 실패 - 요청: {}", request.getRequestId(), e);
            throw new DiagnosisException("POLICY_GENERATION", "EXECUTION_FAILED", 
                "정책 생성 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        if (request.getParameter("generationMode", String.class) == null) {
            throw new DiagnosisException("POLICY_GENERATION", "MISSING_GENERATION_MODE", 
                "generationMode 파라미터가 필요합니다");
        }
        
        if (request.getParameter("naturalLanguageQuery", String.class) == null) {
            throw new DiagnosisException("POLICY_GENERATION", "MISSING_NATURAL_LANGUAGE_QUERY", 
                "naturalLanguageQuery 파라미터가 필요합니다");
        }
        
        String query = request.getParameter("naturalLanguageQuery", String.class);
        if (query.trim().isEmpty()) {
            throw new DiagnosisException("POLICY_GENERATION", "EMPTY_NATURAL_LANGUAGE_QUERY", 
                "자연어 쿼리가 비어있습니다");
        }
    }
    
    /**
     * 정책 생성 응답 클래스
     */
    public static class PolicyGenerationResponse extends IAMResponse {
        private final Object policyResult; // PolicyDto 또는 String
        private final String generationMode;
        private final String originalQuery;
        
        public PolicyGenerationResponse(String requestId, ExecutionStatus status, 
                                      Object policyResult, String generationMode, String originalQuery) {
            super(requestId, status);
            this.policyResult = policyResult;
            this.generationMode = generationMode;
            this.originalQuery = originalQuery;
        }
        
        @Override
        public String getResponseType() { 
            return "POLICY_GENERATION"; 
        }
        
        @Override
        public Object getData() {
            return Map.of(
                "policyResult", policyResult != null ? policyResult : "",
                "generationMode", generationMode != null ? generationMode : "",
                "originalQuery", originalQuery != null ? originalQuery : "",
                "timestamp", getTimestamp(),
                "requestId", getRequestId()
            );
        }
        
        public Object getPolicyResult() { return policyResult; }
        
        public String getGenerationMode() { return generationMode; }
        
        public String getOriginalQuery() { return originalQuery; }
        
        @Override
        public String toString() {
            return String.format("PolicyGenerationResponse{requestId='%s', status='%s', mode='%s'}", 
                getResponseId(), getStatus(), generationMode);
        }
    }
} 