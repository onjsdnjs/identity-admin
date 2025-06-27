package io.spring.iam.aiam.session;

import io.spring.iam.aiam.operations.LabExecutionStrategy;
import io.spring.session.MfaSessionRepository;
import io.spring.session.MfaSessionRepository.SessionStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * AI 전략 세션 관리를 위한 확장된 저장소 인터페이스
 * 
 * 🧠 AI 전략 실행에 특화된 세션 관리
 * - 전략 실행 상태 추적
 * - 연구소 할당 정보 관리  
 * - 분산 환경에서의 세션 동기화
 * - AI 실행 메트릭 수집
 */
public interface AIStrategySessionRepository extends MfaSessionRepository {
    
    // ==================== AI 전략 세션 전용 메서드 ====================
    
    /**
     * AI 전략 실행 세션을 생성합니다
     */
    String createStrategySession(LabExecutionStrategy strategy, 
                               Map<String, Object> context,
                               HttpServletRequest request, 
                               HttpServletResponse response);
    
    /**
     * 전략 실행 상태를 업데이트합니다
     */
    void updateStrategyState(String sessionId, 
                           AIStrategyExecutionPhase phase, 
                           Map<String, Object> phaseData);
    
    /**
     * 전략 실행 단계를 업데이트합니다 (별칭 메서드)
     */
    default void updateExecutionPhase(String sessionId, 
                                    AIStrategyExecutionPhase phase, 
                                    Map<String, Object> phaseData) {
        updateStrategyState(sessionId, phase, phaseData);
    }
    
    /**
     * 전략 실행 상태를 조회합니다
     */
    AIStrategySessionState getStrategyState(String sessionId);
    
    /**
     * 연구소 할당 정보를 저장합니다
     */
    void storeLabAllocation(String sessionId, 
                          String labType, 
                          String nodeId, 
                          Map<String, Object> allocation);
    
    /**
     * 연구소 할당 정보를 조회합니다
     */
    AILabAllocation getLabAllocation(String sessionId);
    
    /**
     * 전략 실행 메트릭을 기록합니다
     */
    void recordExecutionMetrics(String sessionId, 
                              AIExecutionMetrics metrics);
    
    /**
     * 실행 중인 전략 세션 목록을 조회합니다
     */
    List<String> getActiveStrategySessions();
    
    /**
     * 특정 노드의 실행 중인 세션을 조회합니다
     */
    List<String> getActiveSessionsByNode(String nodeId);
    
    /**
     * 전략 세션을 다른 노드로 이전합니다
     */
    boolean migrateStrategySession(String sessionId, 
                                 String fromNodeId, 
                                 String toNodeId);
    
    /**
     * 전략 실행 결과를 저장합니다
     */
    void storeExecutionResult(String sessionId, 
                            AIExecutionResult result);
    
    /**
     * 전략 실행 결과를 조회합니다
     */
    AIExecutionResult getExecutionResult(String sessionId);
    
    /**
     * 분산 환경에서의 세션 동기화
     */
    void syncSessionAcrossNodes(String sessionId);
    
    /**
     * AI 전략 세션 통계 조회
     */
    AIStrategySessionStats getAIStrategyStats();
}