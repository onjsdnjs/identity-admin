package io.spring.identityadmin.admin.monitoring.service;

import io.spring.identityadmin.admin.monitoring.dto.SecurityScoreDto;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityScoreCalculatorImpl implements SecurityScoreCalculator {

    private final UserRepository userRepository;

    @Override
    public SecurityScoreDto calculate() {
        List<SecurityScoreDto.ScoreFactor> factors = new ArrayList<>();
        double totalScore = 100.0;

        // 팩터 1: 관리자 MFA 활성화율 (가중치 50%)
        long adminCount = userRepository.findAll().stream().filter(u -> u.getRoleNames().contains("ADMIN")).count();
        if (adminCount > 0) {
            long mfaDisabledAdminCount = userRepository.findAdminsWithMfaDisabled().size();
            double mfaAdminRate = (double)(adminCount - mfaDisabledAdminCount) / adminCount;
            totalScore -= (1 - mfaAdminRate) * 50.0;
            factors.add(new SecurityScoreDto.ScoreFactor("관리자 MFA 활성화율", (int)(mfaAdminRate * 100), 0.5,"가장 중요한 보안 지표입니다."));
        }

        // 팩터 2: 전체 사용자 MFA 활성화율 (가중치 20%)
        long totalUsers = userRepository.count();
        if (totalUsers > 0) {
//            long mfaEnabledUsers = userRepository.countByMfaEnabled(true);
            long mfaEnabledUsers = 1;
            double mfaTotalRate = (double) mfaEnabledUsers / totalUsers;
            totalScore -= (1- mfaTotalRate) * 20.0;
            factors.add(new SecurityScoreDto.ScoreFactor("전체 사용자 MFA 활성화율", (int)(mfaTotalRate * 100), 0.2, "전체 계정의 보안 수준을 나타냅니다."));
        }

        int finalScore = Math.max(0, (int) Math.round(totalScore));
        String summary = finalScore >= 80 ? "시스템 보안이 양호한 상태입니다." : (finalScore >= 50 ? "일부 보안 설정 강화가 필요합니다." : "보안 설정에 대한 시급한 검토가 필요합니다.");

        return new SecurityScoreDto(finalScore, summary, factors);
    }
}
