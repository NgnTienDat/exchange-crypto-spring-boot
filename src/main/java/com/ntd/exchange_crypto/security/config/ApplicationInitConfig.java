package com.ntd.exchange_crypto.security.config;


import com.ntd.exchange_crypto.user.enums.Role;
import com.ntd.exchange_crypto.user.model.User;
import com.ntd.exchange_crypto.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;
    UserService userService;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            var roles = new HashSet<String>();
            roles.add(Role.ADMIN.name());

            User existingAdmin = userService.getUserByEmail("admin@gmail.com");

            if (existingAdmin == null) {
                User user = User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin"))
                        .roles(roles)
                        .fullName("System Admin")
                        .phone("0811111111")
                        .build();
                userService.saveUser(user);
                log.warn("Admin account has been created. Please change the default password: admin");
            } else {
                log.info("Admin user already exists with id: {}", existingAdmin.getId());
            }
        };
    }

}
