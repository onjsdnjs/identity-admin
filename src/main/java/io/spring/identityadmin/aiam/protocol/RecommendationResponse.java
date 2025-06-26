package io.spring.identityadmin.aiam.protocol;

public class RecommendationResponse<T extends IAMContext> extends IAMResponse {
    private List<Recommendation<T>> recommendations;
    private RecommendationReason reason;
}
