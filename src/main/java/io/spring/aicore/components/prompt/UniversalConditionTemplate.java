package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.stereotype.Component;

/**
 * ✅ OCP 준수: 범용 조건 템플릿 생성 프롬프트
 * 
 * 새로운 조건 템플릿 추가 시:
 * 1. 이 클래스를 복사
 * 2. @PromptTemplateConfig 수정
 * 3. 프롬프트 내용 수정
 * 4. PromptGenerator 수정 불필요!
 */
@Component
@PromptTemplateConfig(
    key = "universal_condition_template",
    aliases = {"universal_condition", "범용조건"},
    description = "ABAC 범용 조건 템플릿 생성용 프롬프트"
)
public class UniversalConditionTemplate implements PromptTemplate {
    
    @Override
    public String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
        return """
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
    }
    
    @Override
    public String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
        return """
        🎯 정확히 3개의 범용 조건만 생성하세요:
        
        1. 사용자 인증 상태 확인 - isAuthenticated()
        2. 관리자 역할 확인 - hasRole('ROLE_ADMIN')  
        3. 업무시간 접근 제한 - T(java.time.LocalTime).now().hour >= 9 && T(java.time.LocalTime).now().hour <= 18
        
        ❌ 절대 금지:
        - 4개 이상 생성
        - hasPermission() 사용 (범용 조건에서는 금지)
        - 존재하지 않는 파라미터 사용
        """;
    }
} 