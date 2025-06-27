package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.DomainContext;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

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
    
    /**
     * 특정 단계가 포함되어 있는지 확인합니다
     */
    public boolean hasStep(PipelineStep step) {
        return steps.contains(step);
    }
    
    // Getters
    public List<PipelineStep> getSteps() { return steps; }
    public Map<String, Object> getParameters() { return parameters; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isEnableCaching() { return enableCaching; }
    public boolean isEnableParallelExecution() { return enableParallelExecution; }
    
    /**
     * 빌더 패턴
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<PipelineStep> steps = new ArrayList<>();
        private Map<String, Object> parameters = new HashMap<>();
        private int timeoutSeconds = 300; // 기본 5분
        private boolean enableCaching = false;
        private boolean enableParallelExecution = false;
        
        public Builder addStep(PipelineStep step) {
            this.steps.add(step);
            return this;
        }
        
        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }
        
        public Builder enableCaching(boolean enableCaching) {
            this.enableCaching = enableCaching;
            return this;
        }
        
        public Builder enableParallelExecution(boolean enableParallelExecution) {
            this.enableParallelExecution = enableParallelExecution;
            return this;
        }
        
        public PipelineConfiguration build() {
            return new PipelineConfiguration(steps, parameters, timeoutSeconds, enableCaching, enableParallelExecution);
        }
    }
    
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