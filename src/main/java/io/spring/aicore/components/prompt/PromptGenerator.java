package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 프롬프트 생성기
 * 
 * ✏️ 현재 하드코딩된 프롬프트 생성 로직을 체계화
 * - 도메인별 시스템 프롬프트 생성
 * - 사용자 쿼리 기반 사용자 프롬프트 생성
 * - 컨텍스트 정보 통합
 */
@Component
public class PromptGenerator {
    
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
        
        // 1. 시스템 프롬프트 생성 (현재 하드코딩된 로직 기반)
        String systemPrompt = generateSystemPrompt(request, systemMetadata);
        
        // 2. 사용자 프롬프트 생성 (현재 하드코딩된 로직 기반)
        String userPrompt = generateUserPrompt(request, contextInfo);
        
        // 3. Spring AI Prompt 객체 생성
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        
        // 4. 메타데이터 수집
        Map<String, Object> metadata = Map.of(
            "systemPromptLength", systemPrompt.length(),
            "userPromptLength", userPrompt.length(),
            "generationTime", System.currentTimeMillis()
        );
        
        return new PromptGenerationResult(prompt, systemPrompt, userPrompt, metadata);
    }
    
    /**
     * 시스템 프롬프트 생성 (현재 하드코딩된 로직과 동일)
     */
    private String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
        // IAM 도메인 특화 시스템 프롬프트 (현재 코드에서 추출)
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
    
    /**
     * 사용자 프롬프트 생성 (현재 하드코딩된 로직과 동일)
     */
    private String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
        String naturalLanguageQuery = extractQueryFromRequest(request);
        
        return String.format("""
            **자연어 요구사항:**
            "%s"
            
            **참고 컨텍스트:**
            %s
            
            위 요구사항을 분석하여 정책을 구성해주세요.
            """, naturalLanguageQuery, contextInfo);
    }
    
    /**
     * 요청에서 자연어 쿼리를 추출합니다
     */
    private String extractQueryFromRequest(AIRequest<? extends DomainContext> request) {
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