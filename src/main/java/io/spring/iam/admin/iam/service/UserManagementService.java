package io.spring.iam.admin.iam.service;

import io.spring.iam.domain.dto.UserDto;
import io.spring.iam.domain.dto.UserListDto;

import java.util.List;

public interface UserManagementService {

    void modifyUser(UserDto userDto);
    List<UserListDto> getUsers();
    UserDto getUser(Long id);
    void deleteUser(Long idx);

}
