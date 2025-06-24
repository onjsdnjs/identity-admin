package io.spring.identityadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.File;

@EnableAsync
@SpringBootApplication
public class IdentityAdminApplication {
/*

    static {
        // 애플리케이션 시작 전에 임시 디렉토리 설정
        String tmpDir = System.getProperty("user.dir") + File.separator + "tmp";
        System.setProperty("java.io.tmpdir", tmpDir);

        // 디렉토리가 없으면 생성
        new File(tmpDir).mkdirs();
    }
*/

    public static void main(String[] args) {
        SpringApplication.run(IdentityAdminApplication.class, args);
    }
}

