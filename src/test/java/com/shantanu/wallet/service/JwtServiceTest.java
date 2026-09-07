package com.shantanu.wallet.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateAndExtractEmail() {
        String token = jwtService.generateToken("jwt-user@test.com");
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("jwt-user@test.com");
    }
}
