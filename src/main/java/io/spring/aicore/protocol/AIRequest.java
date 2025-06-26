package io.spring.aicore.protocol;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 시스템으로의 범용 요청 클래스
 * 제네릭을 활용하여 다양한 도메인 컨텍스트를 타입 안전하게 처리
 * 
 * @param <T> 도메인 컨텍스트 타입
 */
public class AIRequest<T extends DomainContext> {
    
    private final String requestId;
    private final LocalDateTime timestamp;
    private final T context;
    private final String operation;
    private final Map<String, Object> parameters;
    private final RequestPriority priority;
    private final RequestType requestType;
    
    private boolean isStreamingRequired = false;
    private int timeoutSeconds = 30;
    private String correlationId;
    
    public AIRequest(T context, String operation) {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.context = context;
        this.operation = operation;
        this.parameters = new ConcurrentHashMap<>();
        this.priority = RequestPriority.NORMAL;
        this.requestType = RequestType.STANDARD;
    }
    
    public AIRequest(T context, String operation, RequestPriority priority, RequestType requestType) {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.context = context;
        this.operation = operation;
        this.parameters = new ConcurrentHashMap<>();
        this.priority = priority;
        this.requestType = requestType;
    }
    
    /**
     * 파라미터를 추가합니다
     * @param key 파라미터 키
     * @param value 파라미터 값
     * @return 체이닝을 위한 현재 객체
     */
    public AIRequest<T> withParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }
    
    /**
     * 스트리밍 모드를 설정합니다
     * @param required 스트리밍 필요 여부
     * @return 체이닝을 위한 현재 객체
     */
    public AIRequest<T> withStreaming(boolean required) {
        this.isStreamingRequired = required;
        return this;
    }
    
    /**
     * 타임아웃을 설정합니다
     * @param seconds 타임아웃 초
     * @return 체이닝을 위한 현재 객체
     */
    public AIRequest<T> withTimeout(int seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }
    
    /**
     * 상관관계 ID를 설정합니다
     * @param correlationId 상관관계 ID
     * @return 체이닝을 위한 현재 객체
     */
    public AIRequest<T> withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    /**
     * 파라미터를 타입 안전하게 조회합니다
     * @param key 파라미터 키
     * @param type 파라미터 타입
     * @return 파라미터 값 (존재하지 않으면 null)
     */
    @SuppressWarnings("unchecked")
    public <P> P getParameter(String key, Class<P> type) {
        Object value = parameters.get(key);
        return type.isInstance(value) ? (P) value : null;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public T getContext() { return context; }
    public String getOperation() { return operation; }
    public Map<String, Object> getParameters() { return Map.copyOf(parameters); }
    public RequestPriority getPriority() { return priority; }
    public RequestType getRequestType() { return requestType; }
    public boolean isStreamingRequired() { return isStreamingRequired; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public String getCorrelationId() { return correlationId; }
    
    /**
     * 요청 우선순위 열거형
     */
    public enum RequestPriority {
        LOW(1), NORMAL(5), HIGH(8), CRITICAL(10);
        
        private final int level;
        
        RequestPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() { return level; }
    }
    
    /**
     * 요청 타입 열거형
     */
    public enum RequestType {
        STANDARD,           // 표준 요청
        STREAMING,          // 스트리밍 요청
        BATCH,              // 배치 요청
        ANALYSIS,           // 분석 요청
        GENERATION,         // 생성 요청
        VALIDATION          // 검증 요청
    }
    
    @Override
    public String toString() {
        return String.format("AIRequest{id='%s', operation='%s', domain='%s', priority=%s}", 
                requestId, operation, context.getDomainType(), priority);
    }
} 