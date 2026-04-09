package com.internship.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InternshipPlatformApplicationTests {

    @Test
    void contextLoads() {
        // Validates that the Spring context starts correctly with test config.
        // If this fails, there is a misconfiguration in beans, security, or JPA.
    }
}
