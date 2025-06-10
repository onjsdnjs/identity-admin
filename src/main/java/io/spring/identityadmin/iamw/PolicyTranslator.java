package io.spring.identityadmin.iamw;

import io.spring.identityadmin.domain.dto.EntitlementDto;
import io.spring.identityadmin.entity.policy.Policy;
import io.spring.identityadmin.entity.policy.PolicyCondition;
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
public class PolicyTranslator {

    private final SpelExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Policy 객체 하나를 EntitlementDto 스트림으로 번역합니다.
     * 하나의 정책은 여러 규칙(OR)을, 하나의 규칙은 여러 조건(AND)을 가질 수 있습니다.
     */
    public Stream<EntitlementDto> translate(Policy policy, String resourceName) {
        return policy.getRules().stream().map(rule -> {
            // Rule 내의 모든 Condition 들을 AND 관계로 묶어 하나의 큰 ExpressionNode로 만듭니다.
            List<ExpressionNode> conditionNodes = rule.getConditions().stream()
                    .map(this::parseCondition)
                    .collect(Collectors.toList());

            ExpressionNode rootNode = (conditionNodes.size() == 1) ?
                    conditionNodes.getFirst() :
                    new LogicalNode("AND", conditionNodes);

            // 최종 분석된 노드에서 정보를 추출하여 DTO 생성
            Set<String> authorities = rootNode.getRequiredAuthorities();

            // 주체(Role/Group)와 행위(Permission)를 분리
            List<String> subjects = authorities.stream().filter(a -> a.startsWith("ROLE_")).collect(Collectors.toList());
            List<String> actions = authorities.stream().filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toList());

            // 실제 시스템에서는 이 이름들을 DB에서 조회하여 사용자 친화적 이름으로 바꿔야 합니다.
            String subjectName = subjects.isEmpty() ? "모든 인증된 사용자" : String.join(", ", subjects);

            return new EntitlementDto(
                    policy.getId(),
                    subjectName,
                    "N/A", // Type
                    resourceName,
                    actions,
                    List.of(rootNode.getConditionDescription())
            );
        });
    }

    private ExpressionNode parseCondition(PolicyCondition condition) {
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
            }
        }
        return args;
    }
}