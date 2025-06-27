package io.spring.aicore.pipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 파이프라인 실행 컨텍스트
 * 파이프라인 실행 중 각 단계 간 데이터를 공유하는 컨텍스트
 */
public class PipelineExecutionContext {
    
    private final String requestId;
    private final Map<String, Object> stepResults = new ConcurrentHashMap<>();
    private final Map<String, Object> sharedData = new ConcurrentHashMap<>();
    private final Map<String, Object> parameters;
    private final long startTime = System.currentTimeMillis();
    
    /**
     * 기본 생성자
     */
    public PipelineExecutionContext() {
        this.requestId = "unknown";
        this.parameters = new ConcurrentHashMap<>();
    }
    
    /**
     * 파라미터를 포함한 생성자
     * @param requestId 요청 ID
     * @param parameters 파이프라인 파라미터
     */
    public PipelineExecutionContext(String requestId, Map<String, Object> parameters) {
        this.requestId = requestId;
        this.parameters = parameters != null ? new ConcurrentHashMap<>(parameters) : new ConcurrentHashMap<>();
    }
    
    /**
     * 단계 실행 결과를 저장합니다 (PipelineStep enum용)
     * @param step 파이프라인 단계
     * @param result 실행 결과
     */
    public void addStepResult(PipelineConfiguration.PipelineStep step, Object result) {
        stepResults.put(step.name(), result);
    }
    
    /**
     * 단계 실행 결과를 저장합니다
     * @param stepName 단계명
     * @param result 실행 결과
     */
    public void putStepResult(String stepName, Object result) {
        stepResults.put(stepName, result);
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
        sharedData.put(key, value);
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
     * 요청 ID를 조회합니다
     * @return 요청 ID
     */
    public String getRequestId() {
        return requestId;
    }
} 