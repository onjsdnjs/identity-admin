package io.spring.iam.aiam.strategy;

import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;

/**
 * 🎯 AI 진단 전략 인터페이스
 * 
 * 모든 AI 진단 기능의 공통 인터페이스
 * 각 진단 타입별로 구체적인 전략 클래스가 이 인터페이스를 구현
 * 
 * 🌿 자연의 이치: 
 * - AINativeIAMOperations는 이 인터페이스만 알면 됨
 * - 구체적 구현은 각 전략 클래스가 담당
 * - 새로운 진단 타입 추가 시 AINativeIAMOperations 수정 불필요
 * 
 * @param <T> IAM 컨텍스트 타입
 * @param <R> IAM 응답 타입
 */
public interface DiagnosisStrategy<T extends IAMContext, R extends IAMResponse> {
    
    /**
     * 🎯 이 전략이 지원하는 진단 타입을 반환합니다
     * 
     * @return 지원하는 진단 타입
     */
    DiagnosisType getSupportedType();
    
    /**
     * 🔥 실제 AI 진단을 수행합니다
     * 
     * 각 전략 구현체에서:
     * 1. 요청 데이터 검증 및 전처리
     * 2. 해당 전문 연구소(Lab)에 작업 위임
     * 3. 결과 후처리 및 응답 생성
     * 
     * @param request IAM 요청 (진단에 필요한 모든 데이터 포함)
     * @param responseType 응답 타입 클래스
     * @return 진단 결과 응답
     * @throws DiagnosisException 진단 실행 중 오류 발생 시
     */
    R execute(IAMRequest<T> request, Class<R> responseType) throws DiagnosisException;
    
    /**
     * 🔍 이 전략이 주어진 요청을 처리할 수 있는지 확인합니다
     * 
     * @param request IAM 요청
     * @return 처리 가능 여부
     */
    default boolean canHandle(IAMRequest<T> request) {
        return request.getDiagnosisType() == getSupportedType();
    }
    
    /**
     * 📊 이 전략의 우선순위를 반환합니다 (낮을수록 높은 우선순위)
     * 같은 진단 타입에 여러 전략이 있을 경우 사용
     * 
     * @return 우선순위 (기본값: 100)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 🏷️ 이 전략의 설명을 반환합니다
     * 
     * @return 전략 설명
     */
    default String getDescription() {
        return getSupportedType().getDescription();
    }
} 