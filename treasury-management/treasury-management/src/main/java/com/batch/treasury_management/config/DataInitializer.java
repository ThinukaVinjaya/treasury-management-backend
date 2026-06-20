package com.batch.treasury_management.config;

import com.batch.treasury_management.dto.UserRequest;
import com.batch.treasury_management.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        try {
            if (userService.getAllUsers().isEmpty()) {
                log.info("Creating default SUPER_ADMIN user...");

                UserRequest admin = new UserRequest();
                admin.setUsername("admin");
                admin.setEmail("admin@batch.com");
                admin.setFullName("System Administrator");
                admin.setPassword("admin123");
                admin.setRole("SUPER_ADMIN");

                userService.createUser(admin);

                printSuccessBanner();
                log.info("✅ Default SUPER_ADMIN created successfully!");
            } else {
                log.info("✅ Users already exist. Skipping default admin creation.");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create default admin", e);
            System.err.println("⚠️ Default admin creation failed: " + e.getMessage());
        }
    }

    private void printSuccessBanner() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              ✅ DEFAULT SUPER_ADMIN CREATED                ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Username : admin                                          ║");
        System.out.println("║  Password : admin123                                       ║");
        System.out.println("║                                                            ║");
        System.out.println("║  → Please login and change password immediately!           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }
}