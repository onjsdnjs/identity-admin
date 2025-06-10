package io.spring.identityadmin.iamw;

import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(40)
public class IpAddressFunctionTranslator implements SpelFunctionTranslator {
    @Override
    public boolean supports(String functionName) {
        return "hasIpAddress".equalsIgnoreCase(functionName);
    }

    @Override
    public ExpressionNode translate(String functionName, MethodReference node) {
        List<String> args = extractArguments(node);
        String ip = args.isEmpty() ? "알 수 없는 IP" : args.get(0);
        return new TerminalNode("IP(" + ip + ")에서 접근", false);
    }
}
