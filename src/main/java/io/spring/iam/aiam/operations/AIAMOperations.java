package io.spring.iam.aiam.operations;

import io.spring.aicore.operations.AICoreOperations;
import io.spring.iam.aiam.protocol.IAMContext;

/**
 * IAM AI 운영 인터페이스
 * 
 * - AICoreOperations 표준 메서드들이 진입점
 * - execute(), executeStream(), executeBatch() 등이 절대 변하지 않는 진입점들
 * - 새로운 AI 기능 추가 시 인터페이스 수정 불필요
 * 
 * @param <T> IAM 컨텍스트 타입
 */
public interface AIAMOperations<T extends IAMContext> extends AICoreOperations<T> {}