package io.spring.identityadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class IdentityAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityAdminApplication.class, args);
    }
}

