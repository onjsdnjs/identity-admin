package io.spring.identityadmin.aiam.labs.naming;

public interface ResourceNamingLab {
    <T> NamingSuggestion suggestName(T technicalResource, NamingContext context);
    <T> Map<T, NamingSuggestion> suggestBatch(List<T> resources, NamingContext context);
}