package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

import java.util.List;

/**
 * 검증 응답 클래스
 * 정책 유효성 검증 결과를 담는 응답
 */
public class ValidationResponse extends IAMResponse {
    
    private String validatedPolicyId;
    private boolean isValid;
    private List<ValidationError> validationErrors;
    private List<ValidationWarning> validationWarnings;
    private String validationSummary;
    private Double validationScore;
    
    public ValidationResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
        this.isValid = false;
    }
    
    public ValidationResponse(String requestId, ExecutionStatus status, boolean isValid) {
        super(requestId, status);
        this.isValid = isValid;
    }
    
    @Override
    public Object getData() { 
        return validationSummary; 
    }
    
    @Override
    public String getResponseType() { 
        return "VALIDATION"; 
    }
    
    // Getters and Setters
    public String getValidatedPolicyId() { return validatedPolicyId; }
    public void setValidatedPolicyId(String validatedPolicyId) { this.validatedPolicyId = validatedPolicyId; }
    
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    
    public List<ValidationError> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<ValidationError> validationErrors) { this.validationErrors = validationErrors; }
    
    public List<ValidationWarning> getValidationWarnings() { return validationWarnings; }
    public void setValidationWarnings(List<ValidationWarning> validationWarnings) { this.validationWarnings = validationWarnings; }
    
    public String getValidationSummary() { return validationSummary; }
    public void setValidationSummary(String validationSummary) { this.validationSummary = validationSummary; }
    
    public Double getValidationScore() { return validationScore; }
    public void setValidationScore(Double validationScore) { this.validationScore = validationScore; }
    
    /**
     * 검증 오류를 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class ValidationError {
        private String errorCode;
        private String errorMessage;
        private String severity;
        
        public ValidationError(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.severity = "ERROR";
        }
        
        // Getters and Setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    /**
     * 검증 경고를 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class ValidationWarning {
        private String warningCode;
        private String warningMessage;
        
        public ValidationWarning(String warningCode, String warningMessage) {
            this.warningCode = warningCode;
            this.warningMessage = warningMessage;
        }
        
        // Getters and Setters
        public String getWarningCode() { return warningCode; }
        public void setWarningCode(String warningCode) { this.warningCode = warningCode; }
        
        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResponse{policy='%s', valid=%s, score=%.2f}", 
                validatedPolicyId, isValid, validationScore != null ? validationScore : 0.0);
    }
} 