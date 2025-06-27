package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.stereotype.Component;

/**
 * ✅ OCP 준수: 특화 조건 템플릿 생성 프롬프트
 * 
 * 새로운 특화 조건 템플릿 추가 시:
 * 1. 이 클래스를 복사
 * 2. @PromptTemplateConfig 수정
 * 3. 프롬프트 내용 수정
 * 4. PromptGenerator 수정 불필요!
 */
@Component
@PromptTemplateConfig(
    key = "specific_condition_template",
    aliases = {"specific_condition", "특화조건"},
    description = "ABAC 특화 조건 템플릿 생성용 프롬프트"
)
public class SpecificConditionTemplate implements PromptTemplate {
    
    @Override
    public String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
        return """
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
    }
    
    @Override
    public String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
        return contextInfo; // 메서드 정보를 그대로 전달
    }
} 