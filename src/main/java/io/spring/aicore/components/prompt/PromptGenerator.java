package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧠 AI 프롬프트 생성기
 * 
 * ✅ OCP 준수: 새로운 프롬프트 템플릿을 추가할 때 기존 코드 수정 없이 확장 가능
 * - @PromptTemplateConfig 어노테이션으로 템플릿 자동 등록
 * - 요청 타입별 동적 템플릿 선택
 * - 컨텍스트 기반 프롬프트 생성
 * 
 * 🔄 DIP 준수: PromptTemplate 인터페이스에 의존하여 구체적인 템플릿 구현체와 분리
 * 
 * @author AI-Native IAM System
 * @since 2024-01-20
 */
@Component
public class PromptGenerator {
    
    // 등록된 프롬프트 템플릿들
    private final Map<String, PromptTemplate> promptTemplates = new ConcurrentHashMap<>();
    private final List<PromptTemplate> templateBeans;

    @Autowired
    public PromptGenerator(List<PromptTemplate> templateBeans) {
        this.templateBeans = templateBeans;
    }

    /**
     * 스프링 컨테이너에서 PromptTemplate 빈들을 자동으로 등록합니다
     */
    @PostConstruct
    private void autoRegisterTemplates() {
        // 스프링 빈으로 등록된 템플릿들을 자동 등록
        for (PromptTemplate template : templateBeans) {
            registerTemplateFromBean(template);
        }
        
        // 기본 템플릿이 없으면 추가
        if (!promptTemplates.containsKey("default")) {
            promptTemplates.put("default", new DefaultIAMPolicyTemplate());
        }
    }
    
    /**
     * 템플릿 빈에서 어노테이션을 읽어서 자동 등록
     */
    private void registerTemplateFromBean(PromptTemplate template) {
        Class<?> templateClass = template.getClass();
        
        // @PromptTemplateConfig 어노테이션 확인
        if (templateClass.isAnnotationPresent(PromptTemplateConfig.class)) {
            PromptTemplateConfig config = templateClass.getAnnotation(PromptTemplateConfig.class);
            
            // 주요 키 등록
            promptTemplates.put(config.key(), template);
            
            // 별칭들도 등록
            for (String alias : config.aliases()) {
                promptTemplates.put(alias, template);
            }
        } else {
            // 어노테이션이 없으면 클래스명 기반 등록
            String className = templateClass.getSimpleName();
            String key = className.replace("Template", "").toLowerCase();
            promptTemplates.put(key, template);
        }
    }
    
    /**
     * AI 요청과 컨텍스트를 기반으로 프롬프트를 생성합니다
     * 
     * @param request AI 요청
     * @param contextInfo 검색된 컨텍스트 정보
     * @param systemMetadata 시스템 메타데이터
     * @return 생성된 프롬프트
     */
    public PromptGenerationResult generatePrompt(AIRequest<? extends DomainContext> request, 
                                               String contextInfo, 
                                               String systemMetadata) {
        
        // 1. 요청 타입에 맞는 프롬프트 템플릿 선택
        String templateKey = determineTemplateKey(request);
        PromptTemplate template = promptTemplates.get(templateKey);
        
        if (template == null) {
            template = promptTemplates.get("default");
        }
        
        // 2. 동적 프롬프트 생성
        String systemPrompt = template.generateSystemPrompt(request, systemMetadata);
        String userPrompt = template.generateUserPrompt(request, contextInfo);
        
        // 3. Spring AI Prompt 객체 생성
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        
        // 4. 메타데이터 수집
        Map<String, Object> metadata = Map.of(
            "templateKey", templateKey,
            "systemPromptLength", systemPrompt.length(),
            "userPromptLength", userPrompt.length(),
            "generationTime", System.currentTimeMillis()
        );
        
        return new PromptGenerationResult(prompt, systemPrompt, userPrompt, metadata);
    }
    
    /**
     * 프롬프트 템플릿을 수동으로 등록합니다 (필요시에만 사용)
     */
    public void registerTemplate(String key, PromptTemplate template) {
        promptTemplates.put(key, template);
    }
    
    /**
     * 요청 타입에 따른 템플릿 키 결정
     */
    private String determineTemplateKey(AIRequest<? extends DomainContext> request) {
        String operation = request.getOperation();
        String domainType = request.getContext().getDomainType();
        
        // 우선순위: operation + domain > operation > domain > default
        String specificKey = operation + "_" + domainType;
        if (promptTemplates.containsKey(specificKey)) {
            return specificKey;
        }
        
        if (promptTemplates.containsKey(operation)) {
            return operation;
        }
        
        if (promptTemplates.containsKey(domainType)) {
            return domainType;
        }
        
        return "default";
    }
    
    /**
     * 기본 IAM 정책 템플릿 (내부 구현체)
     */
    private static class DefaultIAMPolicyTemplate implements PromptTemplate {
        @Override
        public String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
            return String.format("""
                당신은 IAM 정책 분석 AI '아비터'입니다. 
                
                🎯 임무: 자연어 요구사항을 분석하여 구체적인 정책 구성 요소로 변환
                
                📋 시스템 정보:
                %s
                
                ⚠️ 절대적 JSON 규칙 (위반 시 시스템 오류 발생):
                1. JSON에는 절대 주석을 포함하지 마세요 (// 또는 /* */ 절대 금지)
                2. JSON 내부에 설명 텍스트 절대 금지
                3. 각 필드는 한 번만 포함 (중복 절대 금지)
                4. 모든 ID는 반드시 숫자만 사용
                5. 문자열 값은 반드시 쌍따옴표로 감싸기
                6. 마지막 항목 뒤에 쉼표 절대 금지
                7. 빈 값은 빈 문자열("")이나 빈 배열([]) 사용
                
                🔥 JSON 파싱 오류 방지를 위한 추가 규칙:
                - 키는 반드시 쌍따옴표로 감싸기: "key"
                - 값도 반드시 적절한 타입으로: "string", 123, true, []
                - 객체나 배열이 비어있으면: {}, []
                - 특수문자는 이스케이프: \", \\, \n
                
                📤 필수 JSON 형식 (정확히 이 형식만 사용):
                
                ===JSON시작===
                {
                  "policyName": "정책이름",
                  "description": "정책설명", 
                  "roleIds": [2],
                  "permissionIds": [3],
                  "conditions": {"1": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]},
                  "aiRiskAssessmentEnabled": false,
                  "requiredTrustScore": 0.7,
                  "customConditionSpel": "",
                  "effect": "ALLOW"
                }
                ===JSON끝===
                
                분석 과정이나 설명은 JSON 블록 앞에 작성하고, JSON은 완벽하게 파싱 가능한 형태로만 작성하세요.
                """, systemMetadata);
        }
        
        @Override
        public String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
            String naturalLanguageQuery = extractQueryFromRequest(request);
            
            return String.format("""
                **자연어 요구사항:**
                "%s"
                
                **참고 컨텍스트:**
                %s
                
                위 요구사항을 분석하여 정책을 구성해주세요.
                """, naturalLanguageQuery, contextInfo);
        }
    }
    
    /**
     * 요청에서 자연어 쿼리를 추출합니다
     */
    private static String extractQueryFromRequest(AIRequest<? extends DomainContext> request) {
        // 현재는 간단하게 구현, 나중에 요청 타입별로 확장 가능
        return request.toString(); // 실제로는 요청에서 자연어 쿼리 추출
    }
    
    /**
     * 프롬프트 생성 결과
     */
    public static class PromptGenerationResult {
        private final Prompt prompt;
        private final String systemPrompt;
        private final String userPrompt;
        private final Map<String, Object> metadata;
        
        public PromptGenerationResult(Prompt prompt, String systemPrompt, String userPrompt, Map<String, Object> metadata) {
            this.prompt = prompt;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.metadata = metadata;
        }
        
        public Prompt getPrompt() { return prompt; }
        public String getSystemPrompt() { return systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
} 