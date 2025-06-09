package io.spring.identityadmin;

import io.spring.identityadmin.domain.dto.UserDto;
import io.spring.identityadmin.entity.Users;
import io.spring.identityadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<String> processRegister(@RequestBody UserDto userDto) {

        Users users = modelMapper.map(userDto, Users.class);
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        users.setMfaEnabled(true);
        userRepository.save(users);

        return ResponseEntity.ok().body("success");
    }

    @GetMapping("/users")
    public String usersPage(Model model) {
        return "users";
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<UserDto>> users() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
        return ResponseEntity.ok().body(users);
    }

}
