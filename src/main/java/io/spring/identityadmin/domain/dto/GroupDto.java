package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.domain.entity.GroupRole;
import io.spring.identityadmin.domain.entity.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private List<Long> selectedRoleIds;
    private int roleCount;
    private int userCount;
    private List<RoleDto> roles;
//    private Set<UserGroup> userGroups = new HashSet<>();
//    private Set<GroupRole> groupRoles = new HashSet<>();
}
