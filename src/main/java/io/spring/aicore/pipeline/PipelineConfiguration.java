package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.DomainContext;
import java.util.Map;
import java.util.List;

/**
 * 파이프라인 실행 설정
 * 
 * @param <T> 도메인 컨텍스트 타입
 */
public class PipelineConfiguration<T extends DomainContext> {
    
    private final List<PipelineStep> steps;
    private final Map<String, Object> parameters;
    private final int timeoutSeconds;
    private final boolean enableCaching;
    private final boolean enableParallelExecution;
    
    public PipelineConfiguration(List<PipelineStep> steps, 
                                Map<String, Object> parameters,
                                int timeoutSeconds,
                                boolean enableCaching,
                                boolean enableParallelExecution) {
        this.steps = steps;
        this.parameters = parameters;
        this.timeoutSeconds = timeoutSeconds;
        this.enableCaching = enableCaching;
        this.enableParallelExecution = enableParallelExecution;
    }
    
    // Getters
    public List<PipelineStep> getSteps() { return steps; }
    public Map<String, Object> getParameters() { return parameters; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isEnableCaching() { return enableCaching; }
    public boolean isEnableParallelExecution() { return enableParallelExecution; }
    
    /**
     * 파이프라인 단계 열거형
     */
    public enum PipelineStep {
        PREPROCESSING,      // 전처리
        CONTEXT_RETRIEVAL,  // 컨텍스트 검색
        PROMPT_GENERATION,  // 프롬프트 생성
        LLM_EXECUTION,      // LLM 실행
        RESPONSE_PARSING,   // 응답 파싱
        POSTPROCESSING      // 후처리
    }
} 