package io.spring.identityadmin.domain.dto;

import lombok.Data;

@Data
public class GroupListDto {
    private Long id;
    private String name;
    private String description;
    private int roleCount;
    private int userCount;
}
