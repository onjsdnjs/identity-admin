package io.spring.identityadmin.admin.service.impl;

import io.spring.identityadmin.admin.repository.BusinessActionRepository;
import io.spring.identityadmin.admin.repository.BusinessResourceRepository;
import io.spring.identityadmin.admin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.admin.service.BusinessMetadataService;
import io.spring.identityadmin.admin.service.RoleService;
import io.spring.identityadmin.entity.*;
import io.spring.identityadmin.admin.repository.GroupRepository;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessMetadataServiceImpl implements BusinessMetadataService {

    private final BusinessResourceRepository businessResourceRepository;
    private final BusinessActionRepository businessActionRepository;
    private final ConditionTemplateRepository conditionTemplateRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleService roleService;

    @Override
    public List<BusinessResource> getAllBusinessResources() {
        return businessResourceRepository.findAll();
    }

    @Override
    public List<BusinessAction> getAllBusinessActions() {
        return businessActionRepository.findAll();
    }

    @Override
    public List<BusinessAction> getActionsForResource(Long businessResourceId) {
        if (businessResourceId == null) {
            return Collections.emptyList();
        }

        Optional<BusinessResource> resourceOptional = businessResourceRepository.findById(businessResourceId);

        // [핵심 수정] 조인 엔티티(BusinessResourceAction)에서 실제 BusinessAction을 추출하여 리스트로 반환
        return resourceOptional.map(businessResource -> businessResource.getAvailableActions().stream()
                .map(BusinessResourceAction::getBusinessAction)
                .collect(Collectors.toList())).orElseGet(Collections::emptyList);
    }

    @Override
    public List<ConditionTemplate> getAllConditionTemplates() {
        return conditionTemplateRepository.findAll();
    }

    @Override
    public List<Users> getAllUsersForPolicy() {
        return userRepository.findAll();
    }

    @Override
    public List<Group> getAllGroupsForPolicy() {
        return groupRepository.findAll();
    }

    @Override
    public List<Role> getAllRoles() {
        return roleService.getRoles();
    }


}
