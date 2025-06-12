package io.spring.identityadmin.admin.support.context.service;

import io.spring.identityadmin.admin.support.context.dto.RecentActivityDto;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;
import java.util.List;

public interface UserContextService {
    void saveWizardProgress(String userSessionId, WizardContext context);
    WizardContext getWizardProgress(String userSessionId);
    void clearWizardProgress(String userSessionId);
    List<RecentActivityDto> getRecentActivities(String username);
}