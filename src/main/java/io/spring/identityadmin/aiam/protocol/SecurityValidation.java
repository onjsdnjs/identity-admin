package io.spring.identityadmin.aiam.protocol;

import java.time.LocalDateTime;

/**
 * 보안 검증 정보 클래스
 * ✅ SRP 준수: 보안 검증 정보 관리만 담당
 */
public class SecurityValidation {
    private boolean validated;
    private String validationLevel;
    private String validator;
    private LocalDateTime validationTimestamp;
    
    public SecurityValidation() {
        this.validated = false;
        this.validationLevel = "NONE";
    }
    
    public void markValidated(String validator, String level) {
        this.validated = true;
        this.validator = validator;
        this.validationLevel = level;
        this.validationTimestamp = LocalDateTime.now();
    }
    
    // Getters
    public boolean isValidated() { return validated; }
    public String getValidationLevel() { return validationLevel; }
    public String getValidator() { return validator; }
    public LocalDateTime getValidationTimestamp() { return validationTimestamp; }
    
    @Override
    public String toString() {
        return String.format("SecurityValidation{validated=%s, level='%s', validator='%s'}", 
                validated, validationLevel, validator);
    }
} 