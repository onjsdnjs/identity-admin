package io.spring.identityadmin.admin.iam.service;

import io.spring.identityadmin.domain.entity.Group;

import java.util.List;
import java.util.Optional;

public interface GroupService {
    Group createGroup(Group group, List<Long> selectedRoleIds);
    Optional<Group> getGroup(Long id);
    List<Group> getAllGroups();
    void deleteGroup(Long id);
    Group updateGroup(Group group, List<Long> selectedRoleIds);
}
