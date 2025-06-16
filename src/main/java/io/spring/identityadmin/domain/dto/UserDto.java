package io.spring.identityadmin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String password;
    private boolean mfaEnabled;
    private List<String> roles;
    private List<String> permissions;
    private List<Long> selectedGroupIds;
}
