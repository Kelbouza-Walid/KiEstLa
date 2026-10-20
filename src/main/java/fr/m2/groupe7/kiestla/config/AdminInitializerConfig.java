package fr.m2.groupe7.kiestla.config;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializerConfig.class);

    @Bean
    public CommandLineRunner initAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.initial-admin.username:}") String adminUsername,
            @Value("${app.initial-admin.password:}") String adminPassword) {

        return args -> {
            if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                return;
            }

            if (userRepository.existsByLogin(adminUsername)) {
                return;
            }

            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setLogin(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            log.info("Initial admin user '{}' created successfully.", adminUsername);
        };
    }
}
