package com.yatrika.shared.config;

import com.yatrika.user.domain.User;
import com.yatrika.user.domain.UserRole;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAdminUser() {
        return args -> {
            // Check if admin already exists by email
            String adminEmail = "admin@yatrika.com";

            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .email(adminEmail)
                        .username("admin")
                        .password(passwordEncoder.encode("admin123")) // Encrypt password
                        .firstName("System")
                        .lastName("Administrator")
                        .role(UserRole.ADMIN)  // ⭐ THIS IS KEY - sets role to ADMIN
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();

                userRepository.save(admin);
                log.info("✅ Admin user created successfully!");
                log.info("📧 Email: admin@yatrika.com");
                log.info("🔑 Password: admin123");
                log.warn("⚠️  CHANGE THIS PASSWORD AFTER FIRST LOGIN!");
            } else {
                log.info("✅ Admin user already exists in database");
            }
        };
    }
}