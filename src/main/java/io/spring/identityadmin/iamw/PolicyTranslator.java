package io.spring.identityadmin.iamw;

import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.admin.repository.PermissionRepository;
import io.spring.identityadmin.admin.repository.RoleRepository;
import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.ast.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DB에 저장된 Policy의 SpEL 규칙을 UI에 표시하기 위한 EntitlementDto로 '번역'하는 클래스.
 * AST 분석을 통해 정확하고 구조적인 번역을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyTranslator {

    private final SpelExpressionParser expressionParser = new SpelExpressionParser();
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;

    private record AnalysisResult(List<String> subjectDescriptions, String subjectType, List<String> actionDescriptions, List<String> conditionDescriptions) {}

    /**
     * Policy 객체 하나를 EntitlementDto 스트림으로 번역합니다.
     * 하나의 정책은 여러 규칙(OR)을, 하나의 규칙은 여러 조건(AND)을 가질 수 있습니다.
     */
    public Stream<EntitlementDto> translate(Policy policy, String resourceName) {
        return policy.getRules().stream().map(rule -> {
            List<ExpressionNode> conditionNodes = rule.getConditions().stream()
                    .map(this::parseCondition)
                    .collect(Collectors.toList());
            ExpressionNode rootNode = (conditionNodes.size() == 1) ? conditionNodes.get(0) : new LogicalNode("AND", conditionNodes);

            // [수정] AST 분석 결과를 기반으로 주체, 행위, 조건의 '설명'을 생성
            AnalysisResult analysis = analyzeNode(rootNode);

            return new EntitlementDto(
                    policy.getId(),
                    String.join(", ", analysis.subjectDescriptions),
                    analysis.subjectType,
                    resourceName,
                    analysis.actionDescriptions,
                    analysis.conditionDescriptions
            );
        });
    }

    // [신규] 분석된 노드를 순회하며 최종 DTO에 필요한 정보를 추출하는 메서드
    private AnalysisResult analyzeNode(ExpressionNode rootNode) {
        List<String> subjectDescs = new ArrayList<>();
        List<String> actionDescs = new ArrayList<>();
        List<String> conditionDescs = new ArrayList<>();
        String subjectType = "N/A";

        Set<String> authorities = rootNode.getRequiredAuthorities();
        for (String auth : authorities) {
            if (auth.startsWith("ROLE_")) {
                // 'ROLE_ADMIN' -> 'ADMIN'
                String roleName = auth.substring(5);
                // DB에서 Role 이름을 조회하여 설명 추가
                String friendlyName = roleRepository.findByRoleName(roleName).map(r -> r.getRoleDesc()).orElse(roleName);
                subjectDescs.add(friendlyName);
                subjectType = "역할";
            } else if (auth.startsWith("GROUP_")) {
                // 'GROUP_1' -> 1L
                Long groupId = Long.parseLong(auth.substring(6));
                // DB에서 Group 이름을 조회하여 설명 추가
                String friendlyName = groupRepository.findById(groupId).map(g -> g.getName()).orElse("ID: " + groupId);
                subjectDescs.add(friendlyName);
                subjectType = "그룹";
            } else {
                // DB에서 Permission 설명을 조회하여 설명 추가
                String friendlyName = permissionRepository.findByName(auth).map(p -> p.getDescription()).orElse(auth);
                actionDescs.add(friendlyName);
            }
        }

        // 인증 조건 등 추가
        if (rootNode.requiresAuthentication() && subjectDescs.isEmpty()) {
            subjectDescs.add("인증된 사용자");
            subjectType = "인증 상태";
        }

        conditionDescs.add(rootNode.getConditionDescription());

        return new AnalysisResult(subjectDescs, subjectType, actionDescs, conditionDescs);
    }

    public ExpressionNode parseCondition(PolicyCondition condition) {
        try {
            Expression expression = expressionParser.parseExpression(condition.getExpression());
            SpelNode ast = ((SpelExpression) expression).getAST();
            return walk(ast);
        } catch (Exception e) {
            log.warn("Could not parse SpEL expression: {}. Treating as opaque condition.", condition.getExpression(), e);
            return new TerminalNode(condition.getExpression()); // 파싱 실패 시 원본 문자열 그대로 반환
        }
    }

    private ExpressionNode walk(SpelNode node) {
        // 논리 연산자 처리
        if (node instanceof OpAnd) return new LogicalNode("AND", getChildren(node));
        if (node instanceof OpOr) return new LogicalNode("OR", getChildren(node));
        if (node instanceof OperatorNot) return new LogicalNode("NOT", getChildren(node));

        // 메서드 호출 처리
        if (node instanceof MethodReference) {
            String methodName = ((MethodReference) node).getName();

            // [수정] 인증 상태 표현식 처리 시, requiresAuthentication 플래그를 true로 설정
            switch (methodName) {
                case "isAuthenticated": return new TerminalNode("인증된 사용자", true);
                case "isFullyAuthenticated": return new TerminalNode("완전 인증 사용자 (Remember-Me 아님)", true);
                case "isAnonymous": return new TerminalNode("익명 사용자", false);
                case "isRememberMe": return new TerminalNode("Remember-Me 인증 사용자", true);
            }

            // [수정] 권한/역할 표현식 처리 시, requiresAuthentication 플래그를 명시적으로 false로 설정
            if (methodName.startsWith("has")) {
                List<String> args = getMethodArguments(node);
                String authorities = String.join(", ", args);
                if (methodName.contains("Role")) {
                    return new TerminalNode("역할(" + authorities + ") 보유", "ROLE_" + args.get(0), false);
                }
                return new TerminalNode("권한(" + authorities + ") 보유", args.get(0), false);
            }

            if ("hasIpAddress".equals(methodName)) {
                String ip = getMethodArguments(node).get(0);
                return new TerminalNode("IP(" + ip + ")에서 접근", false);
            }
        }

        if (node instanceof Identifier) {
            String identifier = ((Identifier) node).toString();
            if ("permitAll".equalsIgnoreCase(identifier)) return new TerminalNode("모든 사용자 허용", false);
            if ("denyAll".equalsIgnoreCase(identifier)) return new TerminalNode("모든 사용자 거부", false);
        }

        return new TerminalNode(node.toStringAST());
    }

    private List<ExpressionNode> getChildren(SpelNode node) {
        List<ExpressionNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(walk(node.getChild(i)));
        }
        return children;
    }

    private List<String> getMethodArguments(SpelNode node) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            SpelNode child = node.getChild(i);
            if (child instanceof StringLiteral) {
                args.add(((StringLiteral) child).getLiteralValue().getValue().toString());
            } else if (child instanceof CompoundExpression) { // hasAnyAuthority('A','B')
                for (int j = 0; j < child.getChildCount(); j++) {
                    if (child.getChild(j) instanceof StringLiteral) {
                        args.add(((StringLiteral) child.getChild(j)).getLiteralValue().getValue().toString());
                    }
                }
            }
        }
        return args;
    }
}