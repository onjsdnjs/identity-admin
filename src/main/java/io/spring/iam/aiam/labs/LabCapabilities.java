package io.spring.iam.aiam.labs;

import java.util.List;
import java.util.Set;

/**
 * 🔬 IAM 연구소 역량 정의
 * 
 * 각 연구소의 기술적 역량과 제한사항을 명확히 정의
 */
public class LabCapabilities {
    
    private final double maxLoad; // 최대 부하 (0.0 ~ 100.0)
    private final int maxConcurrentRequests; // 최대 동시 요청 수
    private final long maxResponseTimeMs; // 최대 응답 시간 (밀리초)
    private final double minAccuracyThreshold; // 최소 정확도 임계값 (0.0 ~ 1.0)
    private final Set<String> supportedAIModels; // 지원하는 AI 모델 목록
    private final Set<String> supportedDataFormats; // 지원하는 데이터 포맷
    private final List<CapabilityFeature> features; // 지원하는 기능 목록
    private final ResourceRequirements resourceRequirements; // 리소스 요구사항
    private final QualityMetrics qualityMetrics; // 품질 메트릭
    
    public LabCapabilities(double maxLoad,
                          int maxConcurrentRequests,
                          long maxResponseTimeMs,
                          double minAccuracyThreshold,
                          Set<String> supportedAIModels,
                          Set<String> supportedDataFormats,
                          List<CapabilityFeature> features,
                          ResourceRequirements resourceRequirements,
                          QualityMetrics qualityMetrics) {
        this.maxLoad = maxLoad;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.minAccuracyThreshold = minAccuracyThreshold;
        this.supportedAIModels = supportedAIModels;
        this.supportedDataFormats = supportedDataFormats;
        this.features = features;
        this.resourceRequirements = resourceRequirements;
        this.qualityMetrics = qualityMetrics;
    }
    
    /**
     * 기본 역량을 가진 연구소 생성
     */
    public static LabCapabilities createBasic() {
        return new LabCapabilities(
            80.0, // maxLoad
            10, // maxConcurrentRequests
            5000, // maxResponseTimeMs (5초)
            0.85, // minAccuracyThreshold (85%)
            Set.of("GPT-4", "Claude-3", "Exaone-3.5"), // supportedAIModels
            Set.of("JSON", "XML", "YAML"), // supportedDataFormats
            List.of(
                CapabilityFeature.REAL_TIME_PROCESSING,
                CapabilityFeature.BATCH_PROCESSING,
                CapabilityFeature.CACHING
            ),
            ResourceRequirements.createStandard(),
            QualityMetrics.createStandard()
        );
    }
    
    /**
     * 고성능 역량을 가진 연구소 생성
     */
    public static LabCapabilities createHighPerformance() {
        return new LabCapabilities(
            95.0, // maxLoad
            50, // maxConcurrentRequests
            2000, // maxResponseTimeMs (2초)
            0.95, // minAccuracyThreshold (95%)
            Set.of("GPT-4", "Claude-4", "Exaone-3.5", "Gemini-Pro", "Custom-Model"),
            Set.of("JSON", "XML", "YAML", "Protobuf", "Avro"),
            List.of(
                CapabilityFeature.REAL_TIME_PROCESSING,
                CapabilityFeature.BATCH_PROCESSING,
                CapabilityFeature.STREAMING_PROCESSING,
                CapabilityFeature.CACHING,
                CapabilityFeature.DISTRIBUTED_PROCESSING,
                CapabilityFeature.AUTO_SCALING,
                CapabilityFeature.LOAD_BALANCING
            ),
            ResourceRequirements.createHighPerformance(),
            QualityMetrics.createHighQuality()
        );
    }
    
    /**
     * 특정 요청을 처리할 수 있는지 확인
     */
    public boolean canHandle(int requestCount, long expectedResponseTime, double requiredAccuracy) {
        return requestCount <= maxConcurrentRequests &&
               expectedResponseTime <= maxResponseTimeMs &&
               requiredAccuracy <= minAccuracyThreshold;
    }
    
    /**
     * AI 모델을 지원하는지 확인
     */
    public boolean supportsAIModel(String modelName) {
        return supportedAIModels.contains(modelName);
    }
    
    /**
     * 데이터 포맷을 지원하는지 확인
     */
    public boolean supportsDataFormat(String format) {
        return supportedDataFormats.contains(format);
    }
    
    /**
     * 특정 기능을 지원하는지 확인
     */
    public boolean hasFeature(CapabilityFeature feature) {
        return features.contains(feature);
    }
    
    /**
     * 현재 부하 상태에서 추가 요청을 받을 수 있는지 확인
     */
    public boolean canAcceptMoreLoad(double currentLoad, int additionalRequests) {
        double projectedLoad = currentLoad + (additionalRequests * 2.0); // 요청당 2% 부하 증가 가정
        return projectedLoad <= maxLoad;
    }
    
    // ==================== 내부 클래스들 ====================
    
    /**
     * 연구소 기능 특성
     */
    public enum CapabilityFeature {
        REAL_TIME_PROCESSING("Real-time Processing", "실시간 요청 처리"),
        BATCH_PROCESSING("Batch Processing", "배치 작업 처리"),
        STREAMING_PROCESSING("Streaming Processing", "스트리밍 데이터 처리"),
        CACHING("Caching", "결과 캐싱"),
        DISTRIBUTED_PROCESSING("Distributed Processing", "분산 처리"),
        AUTO_SCALING("Auto Scaling", "자동 스케일링"),
        LOAD_BALANCING("Load Balancing", "부하 분산"),
        FAULT_TOLERANCE("Fault Tolerance", "장애 허용성"),
        ENCRYPTION("Encryption", "데이터 암호화"),
        AUDIT_LOGGING("Audit Logging", "감사 로깅"),
        MULTI_TENANCY("Multi-tenancy", "멀티 테넌시"),
        API_VERSIONING("API Versioning", "API 버전 관리");
        
        private final String displayName;
        private final String description;
        
        CapabilityFeature(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * 리소스 요구사항
     */
    public static class ResourceRequirements {
        private final int minCpuCores;
        private final long minMemoryMB;
        private final long minDiskSpaceGB;
        private final int minNetworkBandwidthMbps;
        
        public ResourceRequirements(int minCpuCores, long minMemoryMB, 
                                  long minDiskSpaceGB, int minNetworkBandwidthMbps) {
            this.minCpuCores = minCpuCores;
            this.minMemoryMB = minMemoryMB;
            this.minDiskSpaceGB = minDiskSpaceGB;
            this.minNetworkBandwidthMbps = minNetworkBandwidthMbps;
        }
        
        public static ResourceRequirements createStandard() {
            return new ResourceRequirements(4, 8192, 100, 100);
        }
        
        public static ResourceRequirements createHighPerformance() {
            return new ResourceRequirements(16, 32768, 500, 1000);
        }
        
        public boolean isSatisfiedBy(int availableCores, long availableMemoryMB,
                                   long availableDiskGB, int availableBandwidthMbps) {
            return availableCores >= minCpuCores &&
                   availableMemoryMB >= minMemoryMB &&
                   availableDiskGB >= minDiskSpaceGB &&
                   availableBandwidthMbps >= minNetworkBandwidthMbps;
        }
        
        // Getters
        public int getMinCpuCores() { return minCpuCores; }
        public long getMinMemoryMB() { return minMemoryMB; }
        public long getMinDiskSpaceGB() { return minDiskSpaceGB; }
        public int getMinNetworkBandwidthMbps() { return minNetworkBandwidthMbps; }
    }
    
    /**
     * 품질 메트릭
     */
    public static class QualityMetrics {
        private final double targetAccuracy; // 목표 정확도
        private final double targetPrecision; // 목표 정밀도
        private final double targetRecall; // 목표 재현율
        private final double targetF1Score; // 목표 F1 점수
        private final long targetResponseTime; // 목표 응답 시간
        private final double targetThroughput; // 목표 처리량 (requests/sec)
        
        public QualityMetrics(double targetAccuracy, double targetPrecision,
                            double targetRecall, double targetF1Score,
                            long targetResponseTime, double targetThroughput) {
            this.targetAccuracy = targetAccuracy;
            this.targetPrecision = targetPrecision;
            this.targetRecall = targetRecall;
            this.targetF1Score = targetF1Score;
            this.targetResponseTime = targetResponseTime;
            this.targetThroughput = targetThroughput;
        }
        
        public static QualityMetrics createStandard() {
            return new QualityMetrics(0.85, 0.80, 0.80, 0.80, 3000, 10.0);
        }
        
        public static QualityMetrics createHighQuality() {
            return new QualityMetrics(0.95, 0.90, 0.90, 0.90, 1000, 50.0);
        }
        
        public boolean meetsQualityStandards(double actualAccuracy, double actualPrecision,
                                           double actualRecall, long actualResponseTime,
                                           double actualThroughput) {
            return actualAccuracy >= targetAccuracy &&
                   actualPrecision >= targetPrecision &&
                   actualRecall >= targetRecall &&
                   actualResponseTime <= targetResponseTime &&
                   actualThroughput >= targetThroughput;
        }
        
        // Getters
        public double getTargetAccuracy() { return targetAccuracy; }
        public double getTargetPrecision() { return targetPrecision; }
        public double getTargetRecall() { return targetRecall; }
        public double getTargetF1Score() { return targetF1Score; }
        public long getTargetResponseTime() { return targetResponseTime; }
        public double getTargetThroughput() { return targetThroughput; }
    }
    
    // ==================== Getters ====================
    
    public double getMaxLoad() { return maxLoad; }
    public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public long getMaxResponseTimeMs() { return maxResponseTimeMs; }
    public double getMinAccuracyThreshold() { return minAccuracyThreshold; }
    public Set<String> getSupportedAIModels() { return supportedAIModels; }
    public Set<String> getSupportedDataFormats() { return supportedDataFormats; }
    public List<CapabilityFeature> getFeatures() { return features; }
    public ResourceRequirements getResourceRequirements() { return resourceRequirements; }
    public QualityMetrics getQualityMetrics() { return qualityMetrics; }
} 