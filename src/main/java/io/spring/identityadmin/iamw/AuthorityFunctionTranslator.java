package io.spring.identityadmin.iamw;


import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(30)
public class AuthorityFunctionTranslator implements SpelFunctionTranslator {
    @Override
    public boolean supports(String functionName) {
        return functionName.toLowerCase().contains("authority");
    }

    @Override
    public ExpressionNode translate(String functionName, MethodReference node) {
        List<String> authorities = extractArguments(node);
        String authorityNames = String.join(", ", authorities);
        return new TerminalNode("권한(" + authorityNames + ") 보유", authorityNames, true);
    }
}
