package io.spring.identityadmin.admin.service;

import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.domain.dto.UserListDto;

import java.util.List;

public interface UserManagementService {

    void modifyUser(UserDto userDto);

    List<UserListDto> getUsers();
    UserDto getUser(Long id);

    void deleteUser(Long idx);

}
