package tz.co.iseke.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.co.iseke.entity.User;
import tz.co.iseke.enums.UserRole;
import tz.co.iseke.repository.UserRepository;

@Configuration
public class ApplicationConfig {

    @Bean
    public CommandLineRunner initializeDefaultUser(UserRepository userRepository,
                                                   PasswordEncoder passwordEncoder) {
        return args -> {
            var existing = userRepository.findByUsername("admin");
            if (existing.isPresent()) {
                User admin = existing.get();
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                userRepository.save(admin);
                System.out.println("Admin password reset to: admin/admin123");
            } else {
                User admin = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .firstName("Francis")
                        .lastName("Sang'wa")
                        .email("admin@saccos.tz")
                        .phoneNumber("+255000000000")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();

                userRepository.save(admin);
                System.out.println("Default admin user created: admin/admin123");
            }
        };
    }
}