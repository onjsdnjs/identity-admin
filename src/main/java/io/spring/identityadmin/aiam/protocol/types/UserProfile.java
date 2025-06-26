package io.spring.identityadmin.aiam.protocol.types;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 프로필 정보 클래스
 * ✅ SRP 준수: 사용자 프로필 데이터만 담당
 */
public class UserProfile {
    private final String userId;
    private String department;
    private String jobTitle;
    private String manager;
    private LocalDateTime joinDate;
    private boolean isHighRiskUser;
    private Map<String, String> attributes;
    
    public UserProfile(String userId) {
        this.userId = userId;
        this.isHighRiskUser = false;
        this.attributes = new ConcurrentHashMap<>();
    }
    
    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    
    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
    
    public boolean isHighRiskUser() { return isHighRiskUser; }
    public void setHighRiskUser(boolean highRiskUser) { isHighRiskUser = highRiskUser; }
    
    public Map<String, String> getAttributes() { return Map.copyOf(attributes); }
    
    @Override
    public String toString() {
        return String.format("UserProfile{id='%s', dept='%s', title='%s', highRisk=%s}", 
                userId, department, jobTitle, isHighRiskUser);
    }
} 