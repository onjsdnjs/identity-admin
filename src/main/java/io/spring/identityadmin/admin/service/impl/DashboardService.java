package io.spring.identityadmin.admin.service.impl;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PolicyRepository;
import io.spring.identityadmin.admin.repository.RoleRepository;
import io.spring.identityadmin.domain.dto.DashboardDto;
import io.spring.identityadmin.entity.Users;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final PolicyRepository policyRepository;

    public DashboardDto getDashboardData() {
        long userCount = userRepository.count();
        long groupCount = groupRepository.count();
        long roleCount = roleRepository.count();
        long policyCount = policyRepository.count();

        // [정상 동작] 이제 존재하는 메서드를 호출합니다.
        List<DashboardDto.ActivityLog> recentActivities = policyRepository.findTop5ByOrderByIdDesc().stream()
                .map(policy -> new DashboardDto.ActivityLog(
                        "최근",
                        String.format("정책 '%s'이(가) 생성되었습니다.", policy.getName())
                ))
                .toList();

        List<DashboardDto.SecurityAlert> securityAlerts = new ArrayList<>();
        // [정상 동작] 최적화된 쿼리를 사용하는 메서드를 호출합니다.
        List<Users> mfaDisabledAdmins = userRepository.findAdminsWithMfaDisabled();
        if (!mfaDisabledAdmins.isEmpty()) {
            securityAlerts.add(new DashboardDto.SecurityAlert(
                    "CRITICAL",
                    String.format("MFA가 비활성화된 관리자 계정이 %d개 있습니다.", mfaDisabledAdmins.size()),
                    "/admin/workbench?view=subjects" // 워크벤치의 주체 뷰로 링크
            ));
        }

        return DashboardDto.builder()
                .userCount(userCount)
                .groupCount(groupCount)
                .roleCount(roleCount)
                .policyCount(policyCount)
                .recentActivities(recentActivities)
                .securityAlerts(securityAlerts)
                .build();
    }
}