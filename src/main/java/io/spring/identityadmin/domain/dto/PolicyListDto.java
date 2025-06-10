package io.spring.identityadmin.domain.dto;

import lombok.Data;

@Data
public class PolicyListDto {
    private Long id;
    private String name;
    private String description;
    private String effect;
    private int priority;
}
