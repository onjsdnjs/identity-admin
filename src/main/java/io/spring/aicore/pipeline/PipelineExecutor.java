package io.spring.aicore.pipeline;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import reactor.core.publisher.Mono;

/**
 * 파이프라인 실행자 인터페이스
 * 
 * @param <T> 도메인 컨텍스트 타입
 * @param <R> 응답 타입
 */
public interface PipelineExecutor<T extends DomainContext, R extends AIResponse> {
    
    /**
     * 파이프라인 단계를 실행합니다
     * @param request AI 요청
     * @param step 실행할 단계
     * @param context 실행 컨텍스트
     * @return 실행 결과
     */
    Mono<Object> executeStep(AIRequest<T> request, 
                            PipelineConfiguration.PipelineStep step,
                            PipelineExecutionContext context);
    
    /**
     * 최종 응답을 생성합니다
     * @param request 원본 요청
     * @param stepResults 각 단계 실행 결과
     * @return 최종 응답
     */
    Mono<R> buildFinalResponse(AIRequest<T> request, 
                              PipelineExecutionContext stepResults);
    
    /**
     * 실행자가 지원하는 응답 타입을 반환합니다
     * @return 응답 타입
     */
    Class<R> getSupportedResponseType();
} 