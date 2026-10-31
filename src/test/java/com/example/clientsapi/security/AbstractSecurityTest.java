package com.example.clientsapi.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Supplies per-test-process credentials so no reusable secret is stored in the repository. */
public abstract class AbstractSecurityTest {

    protected static final String TEST_USERNAME = "test-" + UUID.randomUUID();
    protected static final String TEST_PASSWORD = UUID.randomUUID().toString();
    private static final String TEST_PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    private static final String TEST_JWT_SECRET = createJwtSecret();

    @DynamicPropertySource
    static void registerSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.auth.username", () -> TEST_USERNAME);
        registry.add("app.security.auth.password-hash", () -> TEST_PASSWORD_HASH);
        registry.add("app.security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.security.jwt.expiration-seconds", () -> 900);
    }

    private static String createJwtSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
