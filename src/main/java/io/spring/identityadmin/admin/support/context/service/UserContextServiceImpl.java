package io.spring.identityadmin.admin.support.context.service;

import io.spring.identityadmin.admin.support.context.dto.RecentActivityDto;
import io.spring.identityadmin.repository.AuditLogRepository;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserContextServiceImpl implements UserContextService {

    private final AuditLogRepository auditLogRepository;
    // 세션 저장을 위한 임시 인메모리 저장소.
    private static final Map<String, WizardContext> wizardSession = new ConcurrentHashMap<>();

    @Override
    public void saveWizardProgress(String userSessionId, WizardContext context) {
        wizardSession.put(userSessionId, context);
    }

    @Override
    public WizardContext getWizardProgress(String userSessionId) {
        WizardContext context = wizardSession.get(userSessionId);
        if (context == null) {
            throw new IllegalStateException("Wizard session not found or expired for ID: " + userSessionId);
        }
        return context;
    }

    @Override
    public void clearWizardProgress(String userSessionId) {
        wizardSession.remove(userSessionId);
    }

    @Override
    public List<RecentActivityDto> getRecentActivities(String username) {
        return auditLogRepository.findTop5ByPrincipalNameOrderByIdDesc(username).stream()
                .map(log -> new RecentActivityDto(log.getAction(), log.getResourceIdentifier(), log.getTimestamp()))
                .collect(Collectors.toList());
    }
}