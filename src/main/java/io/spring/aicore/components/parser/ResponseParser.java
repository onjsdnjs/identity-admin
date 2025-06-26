package io.spring.aicore.components.parser;

public interface ResponseParser {
    ParsedResult parse(String response, Schema schema);
    ValidationResult validate(ParsedResult result);
    ResponseMetadata extractMetadata(String response);
}
