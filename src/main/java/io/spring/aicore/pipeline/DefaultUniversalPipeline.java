package io.spring.aicore.pipeline;

import io.spring.aicore.components.parser.ResponseParser;
import io.spring.aicore.components.prompt.PromptGenerator;
import io.spring.aicore.components.retriever.ContextRetriever;
import io.spring.aicore.components.streaming.StreamingProcessor;
import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.aicore.protocol.DomainContext;
import io.spring.iam.aiam.protocol.response.StringResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
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
    private final Map<Class<?>, ResponseParser> parserMap = new HashMap<>();
    private final ChatModel chatModel;
    
    @Autowired
    public DefaultUniversalPipeline(ContextRetriever contextRetriever,
                                   PromptGenerator promptGenerator,
                                   StreamingProcessor streamingProcessor,
                                   ChatModel chatModel) {
        this.contextRetriever = contextRetriever;
        this.promptGenerator = promptGenerator;
        this.streamingProcessor = streamingProcessor;
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
                if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                    ResponseParser parser = selectParser(responseType);
                    String parsedJson = parser.extractAndCleanJson(llmResponse);
                    if (parsedJson != null && !parsedJson.trim().isEmpty()) {
                        ctx.addStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, parsedJson);
                    } else {
                        log.warn("🔥 JSON 파싱 결과가 비어있음, 빈 JSON 사용");
                        ctx.addStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, "{}");
                    }
                } else {
                    log.warn("🔥 LLM 응답이 비어있음, 빈 JSON 사용");
                    ctx.addStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, "{}");
                }
            }
            
            // 6. POSTPROCESSING 단계 (후처리)
            if (configuration.hasStep(PipelineConfiguration.PipelineStep.POSTPROCESSING)) {
                log.debug("✅ POSTPROCESSING 단계 실행");
                
                String parsedJson = ctx.getStepResult(PipelineConfiguration.PipelineStep.RESPONSE_PARSING, String.class);
                if (parsedJson != null && !parsedJson.trim().isEmpty()) {
                    try {
                        // JSON을 응답 타입으로 변환
                        ResponseParser parser = selectParser(responseType);
                        R response = parser.parseToType(parsedJson, responseType);
                        if (response != null) {
                            ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, response);
                        } else {
                            log.warn("🔥 parseToType 결과가 null, 기본 응답 생성 시도");
                            // 빈 응답 객체 생성 시도
                            try {
                                // AIResponse는 추상 클래스이므로 특별 처리
                                if (responseType == AIResponse.class || AIResponse.class.isAssignableFrom(responseType)) {
                                    // 기본 StringAIResponse 생성
                                    DefaultStringAIResponse defaultResponse = new DefaultStringAIResponse("pipeline-default", "{}");
                                    ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, defaultResponse);
                                } else {
                                    R defaultResponse = responseType.getDeclaredConstructor().newInstance();
                                    ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, defaultResponse);
                                }
                            } catch (Exception e) {
                                log.error("🔥 기본 응답 생성 실패, NULL_RESULT 사용", e);
                                ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, "PARSING_FAILED");
                            }
                        }
                    } catch (Exception e) {
                        log.error("🔥 POSTPROCESSING 중 오류 발생", e);
                        ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, "PROCESSING_ERROR");
                    }
                } else {
                    log.warn("🔥 파싱된 JSON이 비어있음, 처리 건너뜀");
                    ctx.addStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, "NO_JSON_TO_PROCESS");
                }
            }
            
            return ctx;
        })
        .map(ctx -> {
            // 최종 결과 반환
            R finalResult = ctx.getStepResult(PipelineConfiguration.PipelineStep.POSTPROCESSING, responseType);
            
            // null 체크 강화
            if (finalResult == null) {
                log.warn("🔥 POSTPROCESSING 결과가 null, 기본 응답 생성 시도");
                try {
                    // ✅ StringResponse 특별 처리 (최우선)
                    if (responseType.isAssignableFrom(StringResponse.class)) {
                        log.debug("🎯 StringResponse 기본 응답 생성");
                        StringResponse defaultStringResponse = new StringResponse("pipeline-final-default", "{}");
                        finalResult = responseType.cast(defaultStringResponse);
                    }
                    // AIResponse는 추상 클래스이므로 특별 처리
                    else if (responseType == AIResponse.class || AIResponse.class.isAssignableFrom(responseType)) {
                        // 기본 StringAIResponse 생성
                        DefaultStringAIResponse defaultResponse = new DefaultStringAIResponse("pipeline-final-default", "{}");
                        finalResult = responseType.cast(defaultResponse);
                    } else {
                        finalResult = responseType.getDeclaredConstructor().newInstance();
                    }
                } catch (Exception e) {
                    log.error("🔥 기본 응답 생성 실패", e);
                    throw new RuntimeException("Failed to create default response", e);
                }
            }
            
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

    /**
     * 🔧 응답 타입별 Parser 등록
     */
    public void registerParser(Class<?> responseType, ResponseParser parser) {
        parserMap.put(responseType, parser);
        log.debug("🔧 Parser 등록: {} -> {}", responseType.getSimpleName(), parser.getClass().getSimpleName());
    }
    
    /**
     * 🔧 기본 Parser 등록 (호환성)
     */
    public void jsonResponseParser(ResponseParser responseParser) {
        // 기본 ResponseParser로 등록 (ResourceNaming 호환성)
        parserMap.put(Object.class, responseParser);
        log.debug("🔧 기본 Parser 등록: {}", responseParser.getClass().getSimpleName());
    }
    
    /**
     * 🔍 응답 타입에 맞는 Parser 선택
     */
    private ResponseParser selectParser(Class<?> responseType) {
        // 1. 정확한 타입 매치
        ResponseParser parser = parserMap.get(responseType);
        if (parser != null) {
            log.debug("🎯 정확한 Parser 선택: {} -> {}", responseType.getSimpleName(), parser.getClass().getSimpleName());
            return parser;
        }
        
        // 2. 상속 관계 확인
        for (Map.Entry<Class<?>, ResponseParser> entry : parserMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(responseType)) {
                log.debug("🎯 상속 관계 Parser 선택: {} -> {}", responseType.getSimpleName(), entry.getValue().getClass().getSimpleName());
                return entry.getValue();
            }
        }
        
        // 3. 기본 Parser 사용
        ResponseParser defaultParser = parserMap.get(Object.class);
        if (defaultParser != null) {
            log.debug("🔧 기본 Parser 사용: {} -> {}", responseType.getSimpleName(), defaultParser.getClass().getSimpleName());
            return defaultParser;
        }
        
        throw new RuntimeException("No parser registered for response type: " + responseType.getSimpleName());
    }
    
    /**
     * 기본 문자열 응답을 위한 간단한 AIResponse 구현체
     */
    private static class DefaultStringAIResponse extends AIResponse {
        private final String data;
        
        public DefaultStringAIResponse(String requestId, String data) {
            super(requestId, AIResponse.ExecutionStatus.SUCCESS);
            this.data = data;
        }
        
        @Override
        public Object getData() {
            return data;
        }
        
        @Override
        public String getResponseType() {
            return "DEFAULT_STRING_RESPONSE";
        }
    }
}