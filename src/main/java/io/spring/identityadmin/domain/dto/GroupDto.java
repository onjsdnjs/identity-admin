package io.spring.identityadmin.domain.dto;

import io.spring.identityadmin.entity.GroupRole;
import io.spring.identityadmin.entity.UserGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.*;

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
    private Set<UserGroup> userGroups = new HashSet<>();
    private Set<GroupRole> groupRoles = new HashSet<>();
}
