package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.monitoring.dto.RiskIndicatorDto;
import io.spring.identityadmin.admin.monitoring.dto.SecurityScoreDto;
import io.spring.identityadmin.domain.dto.DashboardDto;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.PolicyRepository;
import io.spring.identityadmin.repository.RoleRepository;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service("dashboardServiceImpl")
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final PolicyRepository policyRepository;

    @Override
    public DashboardDto getDashboardData() {
        long userCount = userRepository.count();
        long groupCount = groupRepository.count();
        long roleCount = roleRepository.count();
        long policyCount = policyRepository.count();

        SecurityScoreDto securityScore = calculateSecurityScore();
        List<RiskIndicatorDto> riskIndicators = analyzeRiskIndicators();
        return new DashboardDto(userCount, groupCount, roleCount, policyCount, Collections.emptyList(), riskIndicators, securityScore);
    }

    private SecurityScoreDto calculateSecurityScore() {
        long totalUsers = userRepository.count();
        if (totalUsers == 0) return new SecurityScoreDto(100, "사용자 없음", Collections.emptyList());

        long mfaDisabledAdminCount = userRepository.findAdminsWithMfaDisabled().size();
        long adminCount = userRepository.findAll().stream().filter(u -> u.getRoleNames().contains("ADMIN")).count();
        if (adminCount == 0) return new SecurityScoreDto(100, "관리자 계정이 없어 안전합니다.", Collections.emptyList());

        double mfaRate = (double)(adminCount - mfaDisabledAdminCount) / adminCount;
        int score = (int) (mfaRate * 80) + 20; // MFA 비중 80%

        List<SecurityScoreDto.ScoreFactor> factors = List.of(
                new SecurityScoreDto.ScoreFactor("관리자 MFA 활성화율", (int)(mfaRate*100), "가장 중요한 보안 지표입니다.")
        );

        return new SecurityScoreDto(score, "관리자 계정의 MFA 설정이 보안 점수에 큰 영향을 미칩니다.", factors);
    }

    private List<RiskIndicatorDto> analyzeRiskIndicators() {
        List<RiskIndicatorDto> risks = new ArrayList<>();
        long mfaDisabledAdmins = userRepository.findAdminsWithMfaDisabled().size();
        if (mfaDisabledAdmins > 0) {
            risks.add(new RiskIndicatorDto("CRITICAL", "MFA 미사용 관리자 계정 발견",
                    mfaDisabledAdmins + "명의 관리자 계정에 2단계 인증(MFA)이 설정되지 않아 탈취 위험이 높습니다.", "/admin/users"));
        }
        return risks;
    }
}