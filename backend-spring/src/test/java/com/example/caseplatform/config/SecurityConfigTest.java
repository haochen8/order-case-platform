package com.example.caseplatform.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {
    private Jwt token(String issuer, String audience, String subject, Instant expiry) {
        return Jwt.withTokenValue("test").header("alg", "RS256").issuer(issuer)
                .subject(subject).audience(List.of(audience)).issuedAt(Instant.now().minusSeconds(600))
                .expiresAt(expiry).build();
    }

    @Test
    void validatesIssuerAudienceExpiryAndSubject() {
        var validator = SecurityConfig.tokenValidator("https://issuer.example", "case-platform-api");
        var future = Instant.now().plusSeconds(300);
        assertThat(validator.validate(token("https://issuer.example", "case-platform-api", "user", future)).hasErrors()).isFalse();
        assertThat(validator.validate(token("https://wrong.example", "case-platform-api", "user", future)).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://issuer.example", "other-api", "user", future)).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://issuer.example", "case-platform-api", "user", Instant.now().minusSeconds(300))).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://issuer.example", "case-platform-api", "", future)).hasErrors()).isTrue();
        assertThat(validator.validate(Jwt.withTokenValue("test").header("alg", "RS256")
                .issuer("https://issuer.example").subject("user").expiresAt(future).build()).hasErrors()).isTrue();
        assertThat(validator.validate(Jwt.withTokenValue("test").header("alg", "RS256")
                .issuer("https://issuer.example").subject("user").audience(List.of("case-platform-api"))
                .build()).hasErrors()).isTrue();
    }
}
