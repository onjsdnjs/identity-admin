package io.spring.iam.aiam.labs;

/**
 * 🔬 IAM 연구소 상태 정의
 * 
 * 연구소의 현재 운영 상태를 명확히 표현
 */
public enum LabStatus {
    
    /**
     * 🔄 초기화 중
     * 연구소가 시작되어 초기 설정을 진행하는 상태
     */
    INITIALIZING("Initializing", "Lab is starting up and configuring resources", 0),
    
    /**
     * ✅ 정상 운영
     * 연구소가 완전히 준비되어 정상적으로 작업을 수행할 수 있는 상태
     */
    OPERATIONAL("Operational", "Lab is fully operational and ready for research", 100),
    
    /**
     * ⚠️ 성능 저하
     * 연구소가 작동하지만 성능이나 품질에 문제가 있는 상태
     */
    DEGRADED("Degraded", "Lab is operational but with reduced performance", 70),
    
    /**
     * 🔧 유지보수 중
     * 연구소가 계획된 유지보수나 업그레이드를 진행하는 상태
     */
    MAINTENANCE("Maintenance", "Lab is undergoing scheduled maintenance", 30),
    
    /**
     * ⏸️ 일시 중지
     * 연구소가 일시적으로 작업을 중단한 상태
     */
    SUSPENDED("Suspended", "Lab operations are temporarily suspended", 10),
    
    /**
     * ❌ 실패
     * 연구소에 심각한 문제가 발생하여 작업을 수행할 수 없는 상태
     */
    FAILED("Failed", "Lab has encountered critical errors and cannot operate", 0),
    
    /**
     * 🔄 재시작 중
     * 연구소가 문제 해결을 위해 재시작을 진행하는 상태
     */
    RESTARTING("Restarting", "Lab is restarting to recover from issues", 20),
    
    /**
     * 📊 진단 중
     * 연구소가 자체 진단을 수행하는 상태
     */
    DIAGNOSING("Diagnosing", "Lab is performing self-diagnostics", 40),
    
    /**
     * 🛑 종료 중
     * 연구소가 정상적으로 종료되는 과정에 있는 상태
     */
    SHUTTING_DOWN("Shutting Down", "Lab is gracefully shutting down", 10),
    
    /**
     * 💤 대기 중
     * 연구소가 작업 요청을 기다리는 유휴 상태
     */
    IDLE("Idle", "Lab is waiting for research requests", 90);
    
    private final String displayName;
    private final String description;
    private final int healthScore; // 0-100, 높을수록 건강한 상태
    
    LabStatus(String displayName, String description, int healthScore) {
        this.displayName = displayName;
        this.description = description;
        this.healthScore = healthScore;
    }
    
    /**
     * 상태의 표시 이름을 반환합니다
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 상태의 상세 설명을 반환합니다
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 상태의 건강 점수를 반환합니다 (0-100)
     */
    public int getHealthScore() {
        return healthScore;
    }
    
    /**
     * 연구소가 작업을 수행할 수 있는 상태인지 확인합니다
     */
    public boolean canPerformResearch() {
        return switch (this) {
            case OPERATIONAL, DEGRADED, IDLE -> true;
            case INITIALIZING, MAINTENANCE, SUSPENDED, FAILED, 
                 RESTARTING, DIAGNOSING, SHUTTING_DOWN -> false;
        };
    }
    
    /**
     * 연구소가 건강한 상태인지 확인합니다
     */
    public boolean isHealthy() {
        return healthScore >= 70;
    }
    
    /**
     * 연구소가 위험한 상태인지 확인합니다
     */
    public boolean isCritical() {
        return healthScore <= 30;
    }
    
    /**
     * 상태 전환이 가능한지 확인합니다
     */
    public boolean canTransitionTo(LabStatus targetStatus) {
        return switch (this) {
            case INITIALIZING -> targetStatus == OPERATIONAL || 
                               targetStatus == FAILED ||
                               targetStatus == SHUTTING_DOWN;
            case OPERATIONAL -> targetStatus == DEGRADED || 
                              targetStatus == MAINTENANCE ||
                              targetStatus == SUSPENDED ||
                              targetStatus == IDLE ||
                              targetStatus == FAILED ||
                              targetStatus == SHUTTING_DOWN;
            case DEGRADED -> targetStatus == OPERATIONAL || 
                           targetStatus == MAINTENANCE ||
                           targetStatus == FAILED ||
                           targetStatus == RESTARTING ||
                           targetStatus == DIAGNOSING;
            case MAINTENANCE -> targetStatus == OPERATIONAL || 
                              targetStatus == FAILED ||
                              targetStatus == SHUTTING_DOWN;
            case SUSPENDED -> targetStatus == OPERATIONAL || 
                            targetStatus == FAILED ||
                            targetStatus == RESTARTING ||
                            targetStatus == SHUTTING_DOWN;
            case FAILED -> targetStatus == RESTARTING || 
                         targetStatus == DIAGNOSING ||
                         targetStatus == SHUTTING_DOWN;
            case RESTARTING -> targetStatus == OPERATIONAL || 
                             targetStatus == FAILED ||
                             targetStatus == INITIALIZING;
            case DIAGNOSING -> targetStatus == OPERATIONAL || 
                             targetStatus == DEGRADED ||
                             targetStatus == FAILED ||
                             targetStatus == RESTARTING;
            case SHUTTING_DOWN -> false; // 종료 중에는 다른 상태로 전환 불가
            case IDLE -> targetStatus == OPERATIONAL || 
                       targetStatus == SUSPENDED ||
                       targetStatus == MAINTENANCE ||
                       targetStatus == SHUTTING_DOWN;
        };
    }
    
    /**
     * 상태에 따른 우선순위를 반환합니다 (낮을수록 높은 우선순위)
     */
    public int getPriority() {
        return switch (this) {
            case FAILED -> 1;
            case RESTARTING -> 2;
            case DIAGNOSING -> 3;
            case MAINTENANCE -> 4;
            case DEGRADED -> 5;
            case SUSPENDED -> 6;
            case INITIALIZING -> 7;
            case SHUTTING_DOWN -> 8;
            case IDLE -> 9;
            case OPERATIONAL -> 10;
        };
    }
    
    /**
     * 상태에 따른 색상 코드를 반환합니다 (모니터링 UI용)
     */
    public String getColorCode() {
        return switch (this) {
            case OPERATIONAL -> "#28a745"; // 녹색
            case IDLE -> "#6c757d"; // 회색
            case DEGRADED -> "#ffc107"; // 노란색
            case MAINTENANCE -> "#17a2b8"; // 청록색
            case INITIALIZING, RESTARTING, DIAGNOSING -> "#fd7e14"; // 주황색
            case SUSPENDED -> "#6f42c1"; // 보라색
            case FAILED -> "#dc3545"; // 빨간색
            case SHUTTING_DOWN -> "#343a40"; // 어두운 회색
        };
    }
} 