package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.types.PolicyContext;

import java.util.List;
import java.util.Map;

/**
 * 최적화 요청 클래스
 * 정책 최적화를 위한 요청
 */
public class OptimizationRequest<T extends PolicyContext> extends IAMRequest<T> {
    
    private String targetPolicyId;
    private List<String> optimizationGoals;
    private Map<String, Object> constraints;
    private String optimizationType;
    private boolean preserveSemantics;
    
    public OptimizationRequest(T context) {
        super(context, "OPTIMIZATION");
        this.optimizationType = "PERFORMANCE";
        this.preserveSemantics = true;
    }
    
    // Getters and Setters
    public String getTargetPolicyId() { return targetPolicyId; }
    public void setTargetPolicyId(String targetPolicyId) { this.targetPolicyId = targetPolicyId; }
    
    public List<String> getOptimizationGoals() { return optimizationGoals; }
    public void setOptimizationGoals(List<String> optimizationGoals) { this.optimizationGoals = optimizationGoals; }
    
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    
    public String getOptimizationType() { return optimizationType; }
    public void setOptimizationType(String optimizationType) { this.optimizationType = optimizationType; }
    
    public boolean isPreserveSemantics() { return preserveSemantics; }
    public void setPreserveSemantics(boolean preserveSemantics) { this.preserveSemantics = preserveSemantics; }
    
    @Override
    public String toString() {
        return String.format("OptimizationRequest{policy='%s', type='%s', preserve=%s}", 
                targetPolicyId, optimizationType, preserveSemantics);
    }
} 