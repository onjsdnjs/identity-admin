package io.spring.identityadmin.aiam.protocol.request;

import io.spring.identityadmin.aiam.protocol.IAMRequest;
import io.spring.identityadmin.aiam.protocol.types.PolicyContext;

import java.util.List;
import java.util.Map;

/**
 * 정책 최적화 요청 클래스
 * 정책의 성능, 보안, 복잡도를 최적화하기 위한 요청
 */
public class OptimizationRequest<T extends PolicyContext> extends IAMRequest<T> {
    
    private String targetPolicyId;
    private List<String> optimizationGoals;
    private Map<String, Object> constraints;
    private String optimizationLevel;
    private boolean preserveSemantics;
    
    public OptimizationRequest(T context) {
        super(context, "POLICY_OPTIMIZATION");
        this.preserveSemantics = true;
        this.optimizationLevel = "BALANCED";
    }
    
    // Getters and Setters
    public String getTargetPolicyId() { return targetPolicyId; }
    public void setTargetPolicyId(String targetPolicyId) { this.targetPolicyId = targetPolicyId; }
    
    public List<String> getOptimizationGoals() { return optimizationGoals; }
    public void setOptimizationGoals(List<String> optimizationGoals) { this.optimizationGoals = optimizationGoals; }
    
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    
    public String getOptimizationLevel() { return optimizationLevel; }
    public void setOptimizationLevel(String optimizationLevel) { this.optimizationLevel = optimizationLevel; }
    
    public boolean isPreserveSemantics() { return preserveSemantics; }
    public void setPreserveSemantics(boolean preserveSemantics) { this.preserveSemantics = preserveSemantics; }
    
    @Override
    public String toString() {
        return String.format("OptimizationRequest{policy='%s', level='%s', goals=%s}", 
                targetPolicyId, optimizationLevel, optimizationGoals);
    }
} 