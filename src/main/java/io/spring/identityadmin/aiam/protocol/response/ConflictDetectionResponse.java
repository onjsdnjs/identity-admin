package io.spring.identityadmin.aiam.protocol.response;

import io.spring.identityadmin.aiam.protocol.IAMResponse;

import java.util.List;

/**
 * 정책 충돌 감지 응답 클래스
 * 정책 간 충돌 분석 결과를 담는 응답
 */
public class ConflictDetectionResponse extends IAMResponse {
    
    private boolean hasConflicts;
    private List<PolicyConflict> detectedConflicts;
    private String conflictSummary;
    private List<String> resolutionSuggestions;
    
    public ConflictDetectionResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
        this.hasConflicts = false;
    }
    
    public ConflictDetectionResponse(String requestId, ExecutionStatus status, List<PolicyConflict> conflicts) {
        super(requestId, status);
        this.detectedConflicts = conflicts;
        this.hasConflicts = conflicts != null && !conflicts.isEmpty();
    }
    
    @Override
    public Object getData() { 
        return detectedConflicts; 
    }
    
    @Override
    public String getResponseType() { 
        return "CONFLICT_DETECTION"; 
    }
    
    // Getters and Setters
    public boolean hasConflicts() { return hasConflicts; }
    public void setHasConflicts(boolean hasConflicts) { this.hasConflicts = hasConflicts; }
    
    public List<PolicyConflict> getDetectedConflicts() { return detectedConflicts; }
    public void setDetectedConflicts(List<PolicyConflict> detectedConflicts) { 
        this.detectedConflicts = detectedConflicts;
        this.hasConflicts = detectedConflicts != null && !detectedConflicts.isEmpty();
    }
    
    public String getConflictSummary() { return conflictSummary; }
    public void setConflictSummary(String conflictSummary) { this.conflictSummary = conflictSummary; }
    
    public List<String> getResolutionSuggestions() { return resolutionSuggestions; }
    public void setResolutionSuggestions(List<String> resolutionSuggestions) { this.resolutionSuggestions = resolutionSuggestions; }
    
    /**
     * 정책 충돌 정보를 담는 내부 클래스
     * 단순한 데이터 홀더이므로 내부 클래스로 적절함
     */
    public static class PolicyConflict {
        private String conflictType;
        private String description;
        private String policy1Id;
        private String policy2Id;
        private String severity;
        
        public PolicyConflict(String conflictType, String description, String policy1Id, String policy2Id) {
            this.conflictType = conflictType;
            this.description = description;
            this.policy1Id = policy1Id;
            this.policy2Id = policy2Id;
            this.severity = "MEDIUM";
        }
        
        // Getters and Setters
        public String getConflictType() { return conflictType; }
        public void setConflictType(String conflictType) { this.conflictType = conflictType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getPolicy1Id() { return policy1Id; }
        public void setPolicy1Id(String policy1Id) { this.policy1Id = policy1Id; }
        
        public String getPolicy2Id() { return policy2Id; }
        public void setPolicy2Id(String policy2Id) { this.policy2Id = policy2Id; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    @Override
    public String toString() {
        return String.format("ConflictDetectionResponse{hasConflicts=%s, conflictCount=%d}", 
                hasConflicts, detectedConflicts != null ? detectedConflicts.size() : 0);
    }
} 