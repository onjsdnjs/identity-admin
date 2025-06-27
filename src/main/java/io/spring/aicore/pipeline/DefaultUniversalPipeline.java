package io.spring.aicore.pipeline;

import io.spring.aicore.components.parser.JsonResponseParser;
import io.spring.aicore.components.prompt.PromptGenerator;
import io.spring.aicore.components.retriever.ContextRetriever;
import io.spring.aicore.components.streaming.StreamingProcessor;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 기본 범용 AI 파이프라인 구현체
 * 
 * 🚀 현재 하드코딩된 6단계 처리 로직을 체계화
 * - CONTEXT_RETRIEVAL: RAG 검색
 * - PREPROCESSING: 메타데이터 구성
 * - PROMPT_GENERATION: 프롬프트 생성
 * - LLM_EXECUTION: AI 모델 실행
 * - RESPONSE_PARSING: JSON 파싱
 * - POSTPROCESSING: 후처리 및 검증
 */
@Slf4j
@Component
public class DefaultUniversalPipeline implements UniversalPipeline {
    
    private final ContextRetriever contextRetriever;
    private final PromptGenerator promptGenerator;
    private final StreamingProcessor streamingProcessor;
    private final JsonResponseParser jsonResponseParser;
    private final ChatModel chatModel;
    
    @Autowired
    public DefaultUniversalPipeline(ContextRetriever contextRetriever,
                                   PromptGenerator promptGenerator,
                                   StreamingProcessor streamingProcessor,
                                   JsonResponseParser jsonResponseParser,
                                   ChatModel chatModel) {
        this.contextRetriever = contextRetriever;
        this.promptGenerator = promptGenerator;
        this.streamingProcessor = streamingProcessor;
        this.jsonResponseParser = jsonResponseParser;
        this.chatModel = chatModel;
    }
    
    @Override
    public <T extends DomainContext, R extends AIResponse> Mono<R> execute(
            AIRequest<T> request, 
            PipelineConfiguration configuration, 
            Class<R> responseType) {
        
        log.info("🚀 Universal Pipeline 실행 시작: {}", request.getRequestId());
        
        PipelineExecutionContext context = new PipelineExecutionContext(request.getRequestId());
        
        return Mono.fromCallable(() -> {
            
            // 1. CONTEXT_RETRIEVAL 단계 (RAG 검색)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL)) {
                log.debug("🔍 CONTEXT_RETRIEVAL 단계 실행");
                ContextRetriever.ContextRetrievalResult contextResult = contextRetriever.retrieveContext(request);
                context.addStepResult(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL, contextResult);
            }
            
            // 2. PREPROCESSING 단계 (메타데이터 구성)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.PREPROCESSING)) {
                log.debug("📋 PREPROCESSING 단계 실행");
                String systemMetadata = buildSystemMetadata(request);
                context.addStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, systemMetadata);
            }
            
            // 3. PROMPT_GENERATION 단계 (프롬프트 생성)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.PROMPT_GENERATION)) {
                log.debug("✏️ PROMPT_GENERATION 단계 실행");
                
                ContextRetriever.ContextRetrievalResult contextResult = 
                    context.getStepResult(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL, ContextRetriever.ContextRetrievalResult.class);
                String systemMetadata = 
                    context.getStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, String.class);
                
                String contextInfo = contextResult != null ? contextResult.getContextInfo() : "";
                String metadata = systemMetadata != null ? systemMetadata : "";
                
                PromptGenerator.PromptGenerationResult promptResult = 
                    promptGenerator.generatePrompt(request, contextInfo, metadata);
                context.addStepResult(PipelineConfiguration.PipelineStep.PROMPT_GENERATION, promptResult);
            }
            
            return context;
        })
        .flatMap(ctx -> {
            // 4. LLM_EXECUTION 단계 (AI 모델 호출)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.LLM_EXECUTION)) {
                log.debug("🤖 LLM_EXECUTION 단계 실행");
                
                PromptGenerator.PromptGenerationResult promptResult = 
                    ctx.getStepResult(PipelineConfiguration.PipelineStep.PROMPT_GENERATION, PromptGenerator.PromptGenerationResult.class);
                
                if (promptResult != null) {
                    // 스트리밍 처리
                    Flux<String> streamResponse = streamingProcessor.processStream(chatModel, promptResult.getPrompt());
                    
                    // 전체 응답을 수집
                    return streamResponse
                        .collect(StringBuilder::new, StringBuilder::append)
                        .map(StringBuilder::toString)
                        .map(response -> {
                            ctx.addStepResult(PipelineConfiguration.PipelineStep.LLM_EXECUTION, response);
                            return ctx;
                        });
                } else {
                    return Mono.just(ctx);
                }
            } else {
                return Mono.just(ctx);
            }
        })
        .map(ctx -> {
            // 5. RESPONSE_PARSING 단계 (JSON 파싱)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.RESPONSE_PARSING)) {
                log.debug("🔧 RESPONSE_PARSING 단계 실행");
                
                String llmResponse = ctx.getStepResult(PipelineConfiguration.PipelineStep.LLM_EXECUTION, String.class);
                if (llmResponse != null) {
                    String parsedJson = jsonResponseParser.extractAndCleanJson(llmResponse);
                    ctx.addStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, parsedJson);
                }
            }
            
            // 6. POSTPROCESSING 단계 (후처리)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.POSTPROCESSING)) {
                log.debug("✅ POSTPROCESSING 단계 실행");
                
                String parsedJson = ctx.getStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, String.class);
                if (parsedJson != null) {
                    // JSON을 응답 타입으로 변환
                    R response = jsonResponseParser.parseToType(parsedJson, responseType);
                    ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, response);
                }
            }
            
            return ctx;
        })
        .map(ctx -> {
            // 최종 결과 반환
            R finalResult = ctx.getStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, responseType);
            
            log.info("✅ Universal Pipeline 실행 완료: {}", request.getRequestId());
            return finalResult;
        })
        .timeout(Duration.ofMinutes(5))
        .onErrorResume(error -> {
            log.error("❌ Universal Pipeline 실행 실패: {}", error.getMessage(), error);
            return Mono.error(new RuntimeException("Pipeline execution failed", error));
        });
    }
    
    @Override
    public <T extends DomainContext> Flux<String> executeStream(
            AIRequest<T> request, 
            PipelineConfiguration configuration) {
        
        log.info("📡 Universal Pipeline 스트리밍 실행 시작: {}", request.getRequestId());
        
        return Mono.fromCallable(() -> {
            PipelineExecutionContext context = new PipelineExecutionContext(request.getRequestId());
            
            // 1-3단계: 컨텍스트 검색, 전처리, 프롬프트 생성
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL)) {
                ContextRetriever.ContextRetrievalResult contextResult = contextRetriever.retrieveContext(request);
                context.addStepResult(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL, contextResult);
            }
            
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.PREPROCESSING)) {
                String systemMetadata = buildSystemMetadata(request);
                context.addStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, systemMetadata);
            }
            
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.PROMPT_GENERATION)) {
                ContextRetriever.ContextRetrievalResult contextResult = 
                    context.getStepResult(PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL, ContextRetriever.ContextRetrievalResult.class);
                String systemMetadata = 
                    context.getStepResult(PipelineConfiguration.PipelineStep.PREPROCESSING, String.class);
                
                String contextInfo = contextResult != null ? contextResult.getContextInfo() : "";
                String metadata = systemMetadata != null ? systemMetadata : "";
                
                PromptGenerator.PromptGenerationResult promptResult = 
                    promptGenerator.generatePrompt(request, contextInfo, metadata);
                context.addStepResult(PipelineConfiguration.PipelineStep.PROMPT_GENERATION, promptResult);
            }
            
            return context;
        })
        .flatMapMany(context -> {
            // 4단계: 스트리밍 LLM 실행
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.LLM_EXECUTION)) {
                PromptGenerator.PromptGenerationResult promptResult = 
                    context.getStepResult(PipelineConfiguration.PipelineStep.PROMPT_GENERATION, PromptGenerator.PromptGenerationResult.class);
                
                if (promptResult != null) {
                    return streamingProcessor.processStream(chatModel, promptResult.getPrompt());
                }
            }
            
            return Flux.just("Pipeline configuration error");
        })
        .onErrorResume(error -> {
            log.error("❌ Universal Pipeline 스트리밍 실행 실패: {}", error.getMessage(), error);
            return Flux.just("ERROR: Pipeline streaming failed: " + error.getMessage());
        });
    }
    
    @Override
    public boolean supportsConfiguration(PipelineConfiguration configuration) {
        // 모든 기본 단계들을 지원
        return configuration.getSteps().stream()
            .allMatch(step -> step == PipelineConfiguration.PipelineStep.CONTEXT_RETRIEVAL ||
                             step == PipelineConfiguration.PipelineStep.PREPROCESSING ||
                             step == PipelineConfiguration.PipelineStep.PROMPT_GENERATION ||
                             step == PipelineConfiguration.PipelineStep.LLM_EXECUTION ||
                             step == PipelineConfiguration.PipelineStep.RESPONSE_PARSING ||
                             step == PipelineConfiguration.PipelineStep.POSTPROCESSING);
    }
    
    @Override
    public PipelineMetrics getMetrics() {
        return new PipelineMetrics(
            "DefaultUniversalPipeline",
            "1.0.0",
            System.currentTimeMillis(),
            Map.of(
                "supportedSteps", 6,
                "componentsLoaded", 5
            )
        );
    }
    
    /**
     * 시스템 메타데이터 구성 (현재 하드코딩된 로직 기반)
     */
    private <T extends DomainContext> String buildSystemMetadata(AIRequest<T> request) {
        // 현재는 간단하게 구현, 나중에 도메인별로 확장 가능
        return String.format("""
            🎯 시스템 정보:
            - 요청 ID: %s
            - 요청 타입: %s
            - 컨텍스트 타입: %s
            - 처리 시간: %s
            """, 
            request.getRequestId(),
            request.getClass().getSimpleName(),
            request.getContext().getClass().getSimpleName(),
            java.time.LocalDateTime.now()
        );
    }
} 