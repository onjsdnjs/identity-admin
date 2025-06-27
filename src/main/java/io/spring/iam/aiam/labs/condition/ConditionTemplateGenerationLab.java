package io.spring.iam.aiam.labs.condition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조건 템플릿 생성 전문 연구소
 * 
 * 🔬 AINativeIAMSynapseArbiterFromOllama에서 완전히 이식된 기능:
 * - generateUniversalConditionTemplates: 범용 조건 템플릿 생성
 * - generateSpecificConditionTemplates: 특화 조건 템플릿 생성
 * - 모든 프롬프트, 파싱, 폴백 로직 포함
 * 
 * ✅ 원본 코드 100% 동일 - 기존 코드는 절대 변경하지 않음
 */
@Slf4j
@Component
public class ConditionTemplateGenerationLab {
    
    private final OllamaChatModel chatModel;
    
    public ConditionTemplateGenerationLab(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
        log.info("🔬 ConditionTemplateGenerationLab initialized - AINativeIAMSynapseArbiterFromOllama 기능 이식 완료");
    }
    
    /**
     * 🤖 범용 조건 템플릿 생성 
     * 
     * ✅ 원본: AINativeIAMSynapseArbiterFromOllama.generateUniversalConditionTemplates()
     * ✅ 100% 동일한 로직 - 프롬프트, AI 호출, 파싱, 폴백 모두 동일
     */
    public String generateUniversalConditionTemplates() {
        log.info("🤖 AI 범용 조건 템플릿 생성 시작");

        String systemPrompt = """
        당신은 ABAC 범용 조건 생성 전문가입니다.
        반드시 JSON 배열 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.
        
        **필수 JSON 응답 형식:**
        [
          {
            "name": "사용자 인증 상태 확인",
            "description": "사용자 인증 상태를 확인하는 조건",
            "spelTemplate": "isAuthenticated()",
            "category": "인증 상태",
            "classification": "UNIVERSAL"
          }
        ]
        
        **생성할 범용 조건 (정확히 3개만):**
        1. isAuthenticated() - 사용자 인증 상태 확인
        2. hasRole('ROLE_ADMIN') - 관리자 역할 확인  
        3. 업무시간 접근 제한 (9시-18시)
        
        **주의사항:**
        - "~권한" 용어 사용 금지 (시스템 크래시!)
        - "~상태 확인", "~역할 확인", "~접근 제한" 용어 사용
        - 정확히 3개만 생성
        
        🏆 올바른 범용 네이밍 예시:
        - "사용자 인증 상태 확인" ← 올바름
        - "관리자 역할 확인" ← 올바름  
        - "업무시간 접근 제한" ← 올바름
        
        JSON만 출력하세요. 설명 텍스트 금지.
        """;

        String userPrompt = """
        🎯 정확히 3개의 범용 조건만 생성하세요:
        
        1. 사용자 인증 상태 확인 - isAuthenticated()
        2. 관리자 역할 확인 - hasRole('ROLE_ADMIN')  
        3. 업무시간 접근 제한 - T(java.time.LocalTime).now().hour >= 9 && T(java.time.LocalTime).now().hour <= 18
        
        ❌ 절대 금지:
        - 4개 이상 생성
        - hasPermission() 사용 (범용 조건에서는 금지)
        - 존재하지 않는 파라미터 사용
        """;

        try {
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(userPrompt);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 범용 템플릿 응답 수신: {} characters", aiResponse.length());

            // JSON 검증 (원본과 동일)
            String trimmed = aiResponse.trim();
            if (!trimmed.startsWith("[")) {
                log.error("🔥 AI가 JSON 배열이 아닌 형식으로 응답: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                return getFallbackUniversalTemplates();
            }

            return aiResponse;

        } catch (Exception e) {
            log.error("🔥 AI 범용 템플릿 생성 실패", e);
            return getFallbackUniversalTemplates();
        }
    }
    
    /**
     * 🤖 특화 조건 템플릿 생성
     * 
     * ✅ 원본: AINativeIAMSynapseArbiterFromOllama.generateSpecificConditionTemplates()
     * ✅ 100% 동일한 로직 - 프롬프트, AI 호출, 파싱, 폴백 모두 동일
     */
    public String generateSpecificConditionTemplates(String resourceIdentifier, String methodInfo) {
        log.debug("🤖 AI 특화 조건 생성: {}", resourceIdentifier);
        log.info("📝 전달받은 메서드 정보: {}", methodInfo);

        String systemPrompt = """
        🚨 극도로 제한된 ABAC 조건 생성기 🚨
        
        당신은 hasPermission() 전용 조건 생성기입니다.
        반드시 hasPermission(파라미터, 리소스타입, 액션) 형식만 사용하세요.
        
                 🔒 절대적 제약사항:
         1. hasPermission() 함수만 사용 (올바른 형식으로)
         2. 제공된 파라미터만 사용 (추가 파라미터 절대 금지)
         3. 정확히 하나의 조건만 생성 (여러 개 절대 금지)
         4. "~대상 검증", "~접근 확인" 용어만 사용 ("~권한" 절대 금지)
         5. 액션은 CREATE, READ, UPDATE, DELETE만 사용
        
        🚨 경고: 위 제약사항을 위반하면 시스템 오류가 발생합니다!
        """;

        try {
            SystemMessage systemMessage = new SystemMessage(systemPrompt);
            UserMessage userMessage = new UserMessage(methodInfo);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String aiResponse = response.getResult().getOutput().getText();

            log.debug("✅ AI 특화 템플릿 응답 수신: {} characters", aiResponse.length());
            log.info("🔍 AI 응답 전체 내용: {}", aiResponse);

            return aiResponse;

        } catch (Exception e) {
            log.error("🔥 AI 특화 조건 생성 실패: {}", resourceIdentifier, e);
            return generateFallbackHasPermissionCondition(resourceIdentifier, methodInfo);
        }
    }
    
    /**
     * 폴백 범용 템플릿 (원본과 100% 동일)
     */
    private String getFallbackUniversalTemplates() {
        return """
        [
          {
            "name": "사용자 인증 상태 확인",
            "description": "사용자가 인증되었는지 확인하는 조건",
            "spelTemplate": "isAuthenticated()",
            "category": "인증 상태",
            "classification": "UNIVERSAL"
          },
          {
            "name": "관리자 역할 확인",
            "description": "관리자 역할을 가진 사용자인지 확인하는 조건",
            "spelTemplate": "hasRole('ROLE_ADMIN')",
            "category": "역할 확인",
            "classification": "UNIVERSAL"
          },
          {
            "name": "업무시간 접근 제한",
            "description": "오전 9시부터 오후 6시까지만 접근을 허용하는 조건",
            "spelTemplate": "T(java.time.LocalTime).now().hour >= 9 && T(java.time.LocalTime).now().hour <= 18",
            "category": "시간 기반",
            "classification": "UNIVERSAL"
          }
        ]
        """;
    }
    
    /**
     * 폴백 특화 조건 생성 (원본과 100% 동일)
     */
    private String generateFallbackHasPermissionCondition(String resourceIdentifier, String methodInfo) {
        log.warn("🔄 폴백 hasPermission 조건 생성: {}", resourceIdentifier);
        
        return """
        [
          {
            "name": "리소스 접근 확인",
            "description": "리소스에 대한 접근을 확인하는 조건 (폴백 생성)",
            "spelTemplate": "hasPermission(#id, 'RESOURCE', 'READ')",
            "category": "접근 확인",
            "classification": "CONTEXT_DEPENDENT"
          }
        ]
        """;
    }
} 