package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.monitoring.dto.DashboardDto;
import io.spring.identityadmin.admin.monitoring.dto.RiskIndicatorDto;
import io.spring.identityadmin.admin.monitoring.dto.SecurityScoreDto;
import io.spring.identityadmin.admin.support.context.service.UserContextService;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service("dashboardServiceImpl")
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final SecurityScoreCalculator securityScoreCalculator;
    private final PermissionMatrixService permissionMatrixService;


    @Transactional(readOnly = true)
    public DashboardDto getDashboardData() {
        String currentAdminUsername = "onjsdnjs@gmail.com"; // TODO: Spring Security Context에서 실제 사용자 이름 가져오기
        return new DashboardDto(
                userRepository.count(),
                ThreadLocalRandom.current().nextLong(10, 51), // 활성 세션은 임시 랜덤값
                userRepository.countByMfaEnabled(false),
                userRepository.findAdminsWithMfaDisabled().size(),
                userContextService.getRecentActivities(currentAdminUsername),
                analyzeRiskIndicators(),
                securityScoreCalculator.calculate(),
                permissionMatrixService.getPermissionMatrix(null)
        );
    }

    public List<RiskIndicatorDto> analyzeRiskIndicators() {
        List<RiskIndicatorDto> risks = new ArrayList<>();
        long mfaDisabledAdmins = userRepository.findAdminsWithMfaDisabled().size();
        if (mfaDisabledAdmins > 0) {
            risks.add(new RiskIndicatorDto("CRITICAL", "MFA 미사용 관리자 계정 발견",
                    mfaDisabledAdmins + "명의 관리자 계정에 2단계 인증(MFA)이 설정되지 않아 탈취 위험이 높습니다.", "/admin/users"));
        }
        return risks;
    }
}