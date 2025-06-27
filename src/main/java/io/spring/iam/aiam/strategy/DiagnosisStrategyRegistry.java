package io.spring.iam.aiam.strategy;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏭 AI 진단 전략 레지스트리
 * 
 * 모든 DiagnosisStrategy 구현체들을 자동으로 등록하고 관리
 * 
 * 🌿 자연의 이치:
 * - AINativeIAMOperations는 이 레지스트리만 알면 됨
 * - 새로운 전략 추가 시 자동으로 등록됨 (Spring 자동 주입)
 * - AINativeIAMOperations나 DistributedStrategyExecutor 수정 불필요
 */
@Slf4j
@Component
public class DiagnosisStrategyRegistry {
    
    private final Map<DiagnosisType, DiagnosisStrategy<?, ?>> strategies = new HashMap<>();
    
    /**
     * 🔧 스프링이 모든 DiagnosisStrategy 구현체들을 자동으로 찾아서 주입
     * 
     * @param allStrategies 스프링이 찾은 모든 DiagnosisStrategy 구현체들
     */
    public DiagnosisStrategyRegistry(List<DiagnosisStrategy<?, ?>> allStrategies) {
        log.info("🏭 DiagnosisStrategyRegistry 초기화 시작");
        
        for (DiagnosisStrategy<?, ?> strategy : allStrategies) {
            DiagnosisType type = strategy.getSupportedType();
            
            // 중복 전략 체크 (우선순위 기반 교체)
            if (strategies.containsKey(type)) {
                DiagnosisStrategy<?, ?> existing = strategies.get(type);
                if (strategy.getPriority() < existing.getPriority()) {
                    strategies.put(type, strategy);
                    log.info("🔄 진단 전략 교체: {} - {} (우선순위: {} → {})", 
                        type, strategy.getClass().getSimpleName(), 
                        existing.getPriority(), strategy.getPriority());
                } else {
                    log.debug("⏭️ 진단 전략 스킵: {} - {} (낮은 우선순위: {})", 
                        type, strategy.getClass().getSimpleName(), strategy.getPriority());
                }
            } else {
                strategies.put(type, strategy);
                log.info("✅ 진단 전략 등록: {} - {} (우선순위: {})", 
                    type, strategy.getClass().getSimpleName(), strategy.getPriority());
            }
        }
        
        log.info("🎯 DiagnosisStrategyRegistry 초기화 완료: {} 개 전략 등록", strategies.size());
        logRegisteredStrategies();
    }
    
    /**
     * 🔍 진단 타입에 맞는 전략을 찾아서 반환합니다
     * 
     * @param diagnosisType 진단 타입
     * @return 해당 전략 (없으면 예외 발생)
     * @throws DiagnosisException 지원하지 않는 진단 타입인 경우
     */
    public <T extends IAMContext, R extends IAMResponse> DiagnosisStrategy<T, R> getStrategy(DiagnosisType diagnosisType) {
        DiagnosisStrategy<?, ?> strategy = strategies.get(diagnosisType);
        
        if (strategy == null) {
            throw new DiagnosisException(
                diagnosisType != null ? diagnosisType.name() : "NULL",
                "STRATEGY_NOT_FOUND",
                "지원하지 않는 진단 타입입니다: " + diagnosisType
            );
        }
        
        return (DiagnosisStrategy<T, R>) strategy;
    }
    
    /**
     * 🔥 DistributedStrategyExecutor에서 사용할 전략 실행 메서드
     * 
     * @param request IAM 요청
     * @param responseType 응답 타입
     * @return 진단 결과
     * @throws DiagnosisException 진단 실행 중 오류 발생 시
     */
    public <T extends IAMContext, R extends IAMResponse> R executeStrategy(
            IAMRequest<T> request, Class<R> responseType) throws DiagnosisException {
        
        if (request.getDiagnosisType() == null) {
            throw new DiagnosisException("NULL", "MISSING_DIAGNOSIS_TYPE", 
                "요청에 진단 타입이 설정되지 않았습니다");
        }
        
        DiagnosisStrategy<T, R> strategy = getStrategy(request.getDiagnosisType());
        
        log.debug("🎯 진단 실행: {} 전략 사용 - {}", 
            request.getDiagnosisType(), strategy.getClass().getSimpleName());
        
        return strategy.execute(request, responseType);
    }
    
    /**
     * 📊 등록된 모든 전략 정보를 반환합니다
     * 
     * @return 진단 타입별 전략 맵
     */
    public Map<DiagnosisType, String> getRegisteredStrategies() {
        Map<DiagnosisType, String> result = new HashMap<>();
        strategies.forEach((type, strategy) -> 
            result.put(type, strategy.getClass().getSimpleName()));
        return result;
    }
    
    /**
     * 🔍 특정 진단 타입이 지원되는지 확인합니다
     * 
     * @param diagnosisType 진단 타입
     * @return 지원 여부
     */
    public boolean isSupported(DiagnosisType diagnosisType) {
        return strategies.containsKey(diagnosisType);
    }
    
    /**
     * 🔍 특정 작업이 지원되는지 확인합니다
     * 
     * @param operation 작업명
     * @return 지원 여부
     */
    public boolean supportsOperation(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            return false;
        }
        
        // 등록된 전략들의 이름이나 설명에서 작업명 검색
        return strategies.values().stream()
            .anyMatch(strategy -> {
                String strategyName = strategy.getClass().getSimpleName().toLowerCase();
                String operationLower = operation.toLowerCase();
                return strategyName.contains(operationLower) || 
                       strategy.getDescription().toLowerCase().contains(operationLower);
            });
    }
    
    /**
     * 📋 등록된 전략들을 로그로 출력합니다
     */
    private void logRegisteredStrategies() {
        log.info("📋 등록된 AI 진단 전략들:");
        strategies.forEach((type, strategy) -> 
            log.info("  - {}: {} (우선순위: {})", 
                type.getDisplayName(), 
                strategy.getClass().getSimpleName(),
                strategy.getPriority()));
    }
} 