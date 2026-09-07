package com.shantanu.wallet.controller;

import com.shantanu.wallet.AbstractIntegrationTest;
import com.shantanu.wallet.dto.AddMoneyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest extends AbstractIntegrationTest {

    @Test
    void adminCanViewAllWalletsAndTransactions() throws Exception {
        String userToken = registerAndLogin("admin-user@test.com", "password123");
        String adminToken = createAdminAndLogin("admin@test.com", "adminpass");

        AddMoneyRequest addRequest = new AddMoneyRequest(new BigDecimal("75.00"));
        mockMvc.perform(post("/wallet/add")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", "admin-view-add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/wallets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].balance").exists());

        mockMvc.perform(get("/admin/transactions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").exists());
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        String userToken = registerAndLogin("regular@test.com", "password123");

        mockMvc.perform(get("/admin/wallets")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
