package io.spring.identityadmin.admin.service;


import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.entity.Users;

import java.util.List;

public interface UserManagementService {

    void modifyUser(UserDto userDto);

    List<Users> getUsers();
    UserDto getUser(Long id);

    void deleteUser(Long idx);

}
