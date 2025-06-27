package io.spring.iam.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessResourceDto {
    private Long id;
    private String name;
    private String resourceType;
}
