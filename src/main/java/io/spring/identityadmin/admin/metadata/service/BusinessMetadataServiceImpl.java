package io.spring.identityadmin.admin.metadata.service;

import io.spring.identityadmin.repository.BusinessActionRepository;
import io.spring.identityadmin.repository.BusinessResourceRepository;
import io.spring.identityadmin.repository.ConditionTemplateRepository;
import io.spring.identityadmin.admin.iam.service.RoleService;
import io.spring.identityadmin.domain.dto.*;
import io.spring.identityadmin.domain.entity.ConditionTemplate;
import io.spring.identityadmin.domain.entity.business.BusinessAction;
import io.spring.identityadmin.domain.entity.business.BusinessResource;
import io.spring.identityadmin.domain.entity.business.BusinessResourceAction;
import io.spring.identityadmin.repository.GroupRepository;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final ModelMapper modelMapper;

    @Override
    public List<BusinessResourceDto> getAllBusinessResources() {
        return businessResourceRepository.findAll().stream()
                .map(action -> modelMapper.map(action, BusinessResourceDto.class))
                .toList();
    }

    @Override
    public List<BusinessActionDto> getAllBusinessActions() {
        return businessActionRepository.findAll().stream()
                .map(action -> modelMapper.map(action, BusinessActionDto.class))
                .toList();
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
    public List<UserMetadataDto> getAllUsersForPolicy() {
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserMetadataDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupMetadataDto> getAllGroupsForPolicy() {
        return groupRepository.findAll().stream()
                .map(group -> modelMapper.map(group, GroupMetadataDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAllUsersAndGroups() {
        return Map.of(
                "users", getAllUsersForPolicy(),
                "groups", getAllGroupsForPolicy()
        );
    }

    @Override
    public List<RoleMetadataDto> getAllRoles() {
        return roleService.getRoles().stream()
                .map(role -> modelMapper.map(role, RoleMetadataDto.class))
                .collect(Collectors.toList());
    }


}
