package io.spring.aicore.pipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * 파이프라인 실행 컨텍스트
 * 파이프라인 실행 중 각 단계 간 데이터를 공유하는 컨텍스트
 */
public class PipelineExecutionContext {
    
    private final String executionId;
    private final Map<String, Object> parameters;
    private final Map<String, Object> stepResults;
    private final Map<String, Object> sharedData;
    private final long startTime;
    
    /**
     * 기본 생성자
     */
    public PipelineExecutionContext() {
        this.executionId = "unknown";
        this.parameters = new ConcurrentHashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
        this.sharedData = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 파라미터를 포함한 생성자
     * @param executionId 실행 ID
     * @param parameters 파이프라인 파라미터
     */
    public PipelineExecutionContext(String executionId, Map<String, Object> parameters) {
        this.executionId = executionId;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
        this.sharedData = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 새로운 단일 매개변수 생성자
     * @param executionId 실행 ID
     */
    public PipelineExecutionContext(String executionId) {
        this(executionId, new HashMap<>());
    }
    
    /**
     * 단계 실행 결과를 저장합니다 (PipelineStep enum용)
     * @param step 파이프라인 단계
     * @param result 실행 결과
     */
    public void addStepResult(PipelineConfiguration.PipelineStep step, Object result) {
        if (step == null) {
            throw new IllegalArgumentException("Pipeline step cannot be null");
        }
        String stepName = step.name();
        if (stepName == null) {
            throw new IllegalArgumentException("Pipeline step name cannot be null");
        }
        // ConcurrentHashMap은 null 값을 허용하지 않으므로 null 체크
        Object safeResult = result != null ? result : "NULL_RESULT";
        stepResults.put(stepName, safeResult);
    }
    
    /**
     * 단계 실행 결과를 저장합니다
     * @param stepName 단계명
     * @param result 실행 결과
     */
    public void putStepResult(String stepName, Object result) {
        if (stepName == null) {
            throw new IllegalArgumentException("Step name cannot be null");
        }
        // ConcurrentHashMap은 null 값을 허용하지 않으므로 null 체크
        Object safeResult = result != null ? result : "NULL_RESULT";
        stepResults.put(stepName, safeResult);
    }
    
    /**
     * 단계 실행 결과를 조회합니다
     * @param stepName 단계명
     * @param type 결과 타입
     * @return 실행 결과
     */
    @SuppressWarnings("unchecked")
    public <T> T getStepResult(String stepName, Class<T> type) {
        Object result = stepResults.get(stepName);
        // "NULL_RESULT"는 실제로는 null을 의미함
        if ("NULL_RESULT".equals(result)) {
            return null;
        }
        return type.isInstance(result) ? (T) result : null;
    }
    
    /**
     * 파이프라인 단계 결과를 조회합니다
     * @param step 파이프라인 단계
     * @param type 결과 타입
     * @return 실행 결과
     */
    public <T> T getStepResult(PipelineConfiguration.PipelineStep step, Class<T> type) {
        return getStepResult(step.name(), type);
    }
    
    /**
     * 공유 데이터를 저장합니다
     * @param key 키
     * @param value 값
     */
    public void putSharedData(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Shared data key cannot be null");
        }
        // ConcurrentHashMap은 null 값을 허용하지 않으므로 null 체크
        Object safeValue = value != null ? value : "NULL_VALUE";
        sharedData.put(key, safeValue);
    }
    
    /**
     * 공유 데이터를 조회합니다
     * @param key 키
     * @param type 값 타입
     * @return 공유 데이터
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key, Class<T> type) {
        Object value = sharedData.get(key);
        // "NULL_VALUE"는 실제로는 null을 의미함
        if ("NULL_VALUE".equals(value)) {
            return null;
        }
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * 파라미터를 조회합니다
     * @param key 파라미터 키
     * @param type 파라미터 타입
     * @return 파라미터 값
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * 실행 시간을 조회합니다
     * @return 실행 시간 (밀리초)
     */
    public long getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 모든 단계 결과를 조회합니다
     * @return 단계 결과 맵
     */
    public Map<String, Object> getAllStepResults() {
        return Map.copyOf(stepResults);
    }
    
    /**
     * 모든 파라미터를 조회합니다
     * @return 파라미터 맵
     */
    public Map<String, Object> getAllParameters() {
        return Map.copyOf(parameters);
    }
    
    /**
     * 실행 ID를 조회합니다
     * @return 실행 ID
     */
    public String getExecutionId() {
        return executionId;
    }
} 