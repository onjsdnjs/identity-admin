package io.spring.identityadmin.common.event.dto;

/** [신규] 정책 변경 시 발행되는 이벤트 */
public class PolicyChangedEvent extends DomainEvent {
    private final Long policyId;
    public PolicyChangedEvent(Long policyId) { this.policyId = policyId; }
    public Long getPolicyId() { return policyId; }
}
