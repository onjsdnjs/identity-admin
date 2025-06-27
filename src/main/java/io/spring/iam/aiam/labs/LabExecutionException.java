package io.spring.iam.aiam.labs;

/**
 * 🔬 IAM 연구소 실행 예외
 * 
 * 연구소 작업 중 발생하는 예외를 처리
 */
public class LabExecutionException extends RuntimeException {
    
    private final String labId;
    private final String labName;
    private final LabErrorType errorType;
    private final String errorCode;
    
    public LabExecutionException(String message) {
        super(message);
        this.labId = null;
        this.labName = null;
        this.errorType = LabErrorType.GENERAL_ERROR;
        this.errorCode = "LAB_ERROR_001";
    }
    
    public LabExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.labId = null;
        this.labName = null;
        this.errorType = LabErrorType.GENERAL_ERROR;
        this.errorCode = "LAB_ERROR_001";
    }
    
    public LabExecutionException(String labId, String labName, LabErrorType errorType, 
                               String errorCode, String message) {
        super(message);
        this.labId = labId;
        this.labName = labName;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }
    
    public LabExecutionException(String labId, String labName, LabErrorType errorType, 
                               String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.labId = labId;
        this.labName = labName;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }
    
    /**
     * 연구소 오류 타입
     */
    public enum LabErrorType {
        GENERAL_ERROR("General Error", "일반적인 연구소 오류"),
        INITIALIZATION_FAILED("Initialization Failed", "연구소 초기화 실패"),
        RESEARCH_FAILED("Research Failed", "연구 작업 실패"),
        RESOURCE_EXHAUSTED("Resource Exhausted", "리소스 부족"),
        TIMEOUT("Timeout", "처리 시간 초과"),
        VALIDATION_FAILED("Validation Failed", "검증 실패"),
        COLLABORATION_FAILED("Collaboration Failed", "연구소 간 협업 실패"),
        HEALTH_CHECK_FAILED("Health Check Failed", "헬스체크 실패"),
        CONFIGURATION_ERROR("Configuration Error", "설정 오류"),
        EXTERNAL_SERVICE_ERROR("External Service Error", "외부 서비스 오류");
        
        private final String displayName;
        private final String description;
        
        LabErrorType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public String getLabId() { return labId; }
    public String getLabName() { return labName; }
    public LabErrorType getErrorType() { return errorType; }
    public String getErrorCode() { return errorCode; }
    
    @Override
    public String toString() {
        if (labId != null && labName != null) {
            return String.format("LabExecutionException[%s:%s] %s - %s: %s", 
                               labId, labName, errorCode, errorType.getDisplayName(), getMessage());
        } else {
            return String.format("LabExecutionException %s - %s: %s", 
                               errorCode, errorType.getDisplayName(), getMessage());
        }
    }
} 