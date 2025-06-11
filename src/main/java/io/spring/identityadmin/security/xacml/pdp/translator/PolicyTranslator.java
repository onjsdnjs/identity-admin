package io.spring.identityadmin.security.xacml.pdp.translator;

import io.spring.identityadmin.domain.entity.policy.Policy;
import io.spring.identityadmin.domain.entity.policy.PolicyCondition;
import io.spring.identityadmin.domain.entity.policy.PolicyRule;
import io.spring.identityadmin.security.xacml.pip.repository.GroupRepository;
import io.spring.identityadmin.security.xacml.pip.repository.PermissionRepository;
import io.spring.identityadmin.security.xacml.pip.repository.RoleRepository;
import io.spring.identityadmin.domain.dto.EntitlementDto;
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
    private final List<SpelFunctionTranslator> translators;

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

    /**
     * [신규] Policy 전체를 받아, 규칙들을 조합하여 최종 ExpressionNode 트리로 파싱합니다.
     * 이 메서드가 외부에서 호출되는 유일한 진입점입니다.
     * @param policy 분석할 Policy 객체
     * @return 분석된 규칙을 나타내는 최상위 ExpressionNode
     */
    public ExpressionNode parsePolicy(Policy policy) {
        if (policy == null || policy.getRules() == null || policy.getRules().isEmpty()) {
            return new TerminalNode("정의된 규칙 없음");
        }

        // 각 Rule은 AND로 묶인 조건들의 집합이며, Rule 끼리는 OR로 연결됩니다.
        List<ExpressionNode> ruleNodes = policy.getRules().stream()
                .map(this::parseRule)
                .collect(Collectors.toList());

        return (ruleNodes.size() == 1) ? ruleNodes.getFirst() : new LogicalNode("OR", ruleNodes);
    }

    /**
     * PolicyRule 하나를 파싱하여 ExpressionNode로 변환합니다.
     * Rule 내의 Condition들은 AND로 연결됩니다.
     */
    private ExpressionNode parseRule(PolicyRule rule) {
        List<ExpressionNode> conditionNodes = rule.getConditions().stream()
                .map(this::parseCondition)
                .collect(Collectors.toList());

        return (conditionNodes.size() == 1) ? conditionNodes.get(0) : new LogicalNode("AND", conditionNodes);
    }

    private ExpressionNode walk(SpelNode node) {
        // 논리 연산자 처리
        if (node instanceof OpAnd) return new LogicalNode("AND", getChildren(node));
        if (node instanceof OpOr) return new LogicalNode("OR", getChildren(node));
        if (node instanceof OperatorNot) return new LogicalNode("NOT", getChildren(node));

        // [핵심 변경] 메서드 호출 부분을 전략 패턴으로 위임
        if (node instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) node;
            String methodName = methodRef.getName();

            // 주입된 번역기 리스트를 순회하며 적절한 전략을 찾음
            // @Order에 의해 우선순위가 높은 순서대로 순회
            for (SpelFunctionTranslator translator : translators) {
                if (translator.supports(methodName)) {
                    return translator.translate(methodName, methodRef);
                }
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