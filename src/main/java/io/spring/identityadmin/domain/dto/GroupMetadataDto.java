package io.spring.identityadmin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMetadataDto {
    private Long id;
    private String name;
    private String description;
}
