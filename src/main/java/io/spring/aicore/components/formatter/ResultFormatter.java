package io.spring.aicore.components.formatter;

public interface ResultFormatter {
    Object format(ParsedResult parsedResult, FormatSpecification spec);
    boolean supports(FormatType formatType);
}
