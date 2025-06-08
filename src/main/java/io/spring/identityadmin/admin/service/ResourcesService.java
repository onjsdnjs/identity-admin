package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.entity.Resources;
import io.spring.identityadmin.entity.Role;

import java.util.List;
import java.util.Set;

public interface ResourcesService {
    Resources getResources(long id);
    List<Resources> getResources();
    Resources createResources(Resources resources, Set<Role> roles) ;
    Resources updateResources(Resources resources, Set<Role> roles);
    void deleteResources(long id);
}
