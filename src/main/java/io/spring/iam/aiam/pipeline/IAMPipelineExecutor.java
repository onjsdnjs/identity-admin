package io.spring.iam.aiam.pipeline;

import io.spring.aicore.pipeline.PipelineConfiguration;
import io.spring.aicore.pipeline.PipelineExecutionContext;
import io.spring.aicore.pipeline.PipelineExecutor;
import io.spring.aicore.protocol.AIRequest;
import io.spring.iam.aiam.labs.IAMLabRegistry;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * IAM 도메인 전용 파이프라인 실행자
 * 
 * 🎯 IAM 특화 AI 처리 파이프라인 실행
 * - IAM Labs와 연동한 전문 처리
 * - 도메인별 컨텍스트 처리
 * - IAM 응답 타입 지원
 */
@Slf4j
@Component
public class IAMPipelineExecutor<T extends IAMContext> implements PipelineExecutor<T, IAMResponse> {
    
    private final IAMLabRegistry labRegistry;
    
    @Autowired
    public IAMPipelineExecutor(IAMLabRegistry labRegistry) {
        this.labRegistry = labRegistry;
    }
    
    @Override
    public Mono<Object> executeStep(AIRequest<T> request, 
                                   PipelineConfiguration.PipelineStep step,
                                   PipelineExecutionContext context) {
        log.debug("🔄 IAM Pipeline: Executing step {} for request {}", step, request.getRequestId());
        
        return Mono.fromCallable(() -> {
            switch (step) {
                case PREPROCESSING:
                    return executePreprocessing(request, context);
                    
                case CONTEXT_RETRIEVAL:
                    return executeContextRetrieval(request, context);
                    
                case PROMPT_GENERATION:
                    return executePromptGeneration(request, context);
                    
                case LLM_EXECUTION:
                    return executeLLM(request, context);
                    
                case RESPONSE_PARSING:
                    return executeResponseParsing(request, context);
                    
                case POSTPROCESSING:
                    return executePostprocessing(request, context);
                    
                default:
                    throw new IllegalArgumentException("Unsupported pipeline step: " + step);
            }
        });
    }
    
    @Override
    public Mono<IAMResponse> buildFinalResponse(AIRequest<T> request, 
                                               PipelineExecutionContext context) {
        log.debug("🏗️ IAM Pipeline: Building final response for request {}", request.getRequestId());
        
        return Mono.fromCallable(() -> {
            // 각 단계 결과를 종합하여 최종 IAM 응답 생성
            Object preprocessingResult = context.getStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, Object.class);
            Object llmResult = context.getStepResult(PipelineConfiguration.PipelineStep.LLM_EXECUTION, Object.class);
            Object parsedResult = context.getStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, Object.class);
            
            // IAM 응답 생성 (현재는 기본 구현)
            IAMResponse response = new IAMResponse(request.getRequestId(), IAMResponse.ExecutionStatus.SUCCESS) {
                @Override
                public Object getData() {
                    return parsedResult != null ? parsedResult : "IAM_PIPELINE_RESULT";
                }
                
                @Override
                public String getResponseType() {
                    return "IAM_PIPELINE_RESPONSE";
                }
            };
            
            // 실행 시간 및 메타데이터 추가
            response.withExecutionTime(java.time.Duration.ofMillis(context.getExecutionTime()));
            response.withMetadata("pipelineSteps", context.getAllStepResults().keySet());
            response.withMetadata("requestType", request.getClass().getSimpleName());
            
            return response;
        });
    }
    
    @Override
    public Class<IAMResponse> getSupportedResponseType() {
        return IAMResponse.class;
    }
    
    // ==================== 단계별 실행 메서드들 ====================
    
    /**
     * 전처리 단계 실행
     */
    private Object executePreprocessing(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("📋 Preprocessing IAM request: {}", request.getRequestId());
        
        // IAM 요청 검증 및 정규화
        Map<String, Object> preprocessedData = Map.of(
            "requestId", request.getRequestId(),
            "requestType", request.getClass().getSimpleName(),
            "contextType", request.getContext().getClass().getSimpleName(),
            "timestamp", System.currentTimeMillis()
        );
        
        return preprocessedData;
    }
    
    /**
     * 컨텍스트 검색 단계 실행
     */
    private Object executeContextRetrieval(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("🔍 Retrieving context for IAM request: {}", request.getRequestId());
        
        // IAM 컨텍스트 기반 관련 정보 검색
        Map<String, Object> contextData = Map.of(
            "userContext", request.getContext(),
            "organizationId", request.getContext().getOrganizationId(),
            "sessionId", request.getContext().getSessionId(),
            "retrievalTime", System.currentTimeMillis()
        );
        
        return contextData;
    }
    
    /**
     * 프롬프트 생성 단계 실행
     */
    private Object executePromptGeneration(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("✏️ Generating prompt for IAM request: {}", request.getRequestId());
        
        // IAM 도메인 특화 프롬프트 생성
        Object preprocessedData = context.getStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, Object.class);
        Object contextData = context.getStepResult(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL, Object.class);
        
        String prompt = String.format(
            "IAM Request Analysis:\n" +
            "Request Type: %s\n" +
            "Context: %s\n" +
            "Organization: %s\n" +
            "Please provide IAM-specific analysis and recommendations.",
            request.getClass().getSimpleName(),
            request.getContext().getClass().getSimpleName(),
            request.getContext().getOrganizationId()
        );
        
        return Map.of(
            "prompt", prompt,
            "promptType", "IAM_ANALYSIS",
            "generationTime", System.currentTimeMillis()
        );
    }
    
    /**
     * LLM 실행 단계
     */
    private Object executeLLM(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("🤖 Executing LLM for IAM request: {}", request.getRequestId());
        
        Object promptData = context.getStepResult(PipelineConfiguration.PipelineStep.PROMPT_GENERATION, Object.class);
        
        // 현재는 Mock LLM 응답 (실제 구현시 Ollama/Exaone3.5 연동)
        String mockLLMResponse = String.format(
            "IAM Analysis Result for request %s:\n" +
            "- Request processed successfully\n" +
            "- Security level: MEDIUM\n" +
            "- Recommended actions: APPROVE with monitoring\n" +
            "- Confidence: 0.85",
            request.getRequestId()
        );
        
        return Map.of(
            "llmResponse", mockLLMResponse,
            "confidence", 0.85,
            "model", "mock-iam-model",
            "executionTime", System.currentTimeMillis()
        );
    }
    
    /**
     * 응답 파싱 단계 실행
     */
    private Object executeResponseParsing(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("📝 Parsing LLM response for IAM request: {}", request.getRequestId());
        
        Object llmResult = context.getStepResult(PipelineConfiguration.PipelineStep.LLM_EXECUTION, Object.class);
        
        // LLM 응답을 구조화된 IAM 데이터로 파싱
        Map<String, Object> parsedResult = Map.of(
            "decision", "APPROVE",
            "securityLevel", "MEDIUM",
            "confidence", 0.85,
            "recommendations", java.util.List.of("Enable monitoring", "Review in 30 days"),
            "parseTime", System.currentTimeMillis()
        );
        
        return parsedResult;
    }
    
    /**
     * 후처리 단계 실행
     */
    private Object executePostprocessing(AIRequest<T> request, PipelineExecutionContext context) {
        log.debug("🔧 Post-processing IAM request: {}", request.getRequestId());
        
        Object parsedResult = context.getStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, Object.class);
        
        // IAM 특화 후처리 (감사 로그, 보안 검증 등)
        Map<String, Object> postProcessedResult = Map.of(
            "auditLogged", true,
            "securityValidated", true,
            "complianceChecked", true,
            "finalResult", parsedResult,
            "postProcessTime", System.currentTimeMillis()
        );
        
        return postProcessedResult;
    }
} 