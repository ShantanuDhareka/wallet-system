package com.shantanu.wallet.controller;

import com.shantanu.wallet.AbstractIntegrationTest;
import com.shantanu.wallet.dto.AddMoneyRequest;
import com.shantanu.wallet.dto.TransferRequest;
import com.shantanu.wallet.entity.User;
import com.shantanu.wallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void addMoney_getWallet_andIdempotency() throws Exception {
        String token = registerAndLogin("wallet-user@test.com", "password123");

        AddMoneyRequest addRequest = new AddMoneyRequest(new BigDecimal("100.00"));
        mockMvc.perform(post("/wallet/add")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "add-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/wallet")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));

        mockMvc.perform(post("/wallet/add")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "add-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/wallet")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void transfer_success() throws Exception {
        String senderToken = registerAndLogin("sender@test.com", "password123");
        String receiverToken = registerAndLogin("receiver@test.com", "password123");

        User receiver = userRepository.findByEmail("receiver@test.com").orElseThrow();

        AddMoneyRequest addRequest = new AddMoneyRequest(new BigDecimal("200.00"));
        mockMvc.perform(post("/wallet/add")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", "sender-fund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        TransferRequest transferRequest = new TransferRequest(receiver.getId(), new BigDecimal("50.00"));
        mockMvc.perform(post("/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", "transfer-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        mockMvc.perform(get("/wallet")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        mockMvc.perform(get("/wallet")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void transfer_insufficientBalance_returnsBadRequest() throws Exception {
        String senderToken = registerAndLogin("poor-sender@test.com", "password123");
        String receiverToken = registerAndLogin("rich-receiver@test.com", "password123");
        User receiver = userRepository.findByEmail("rich-receiver@test.com").orElseThrow();

        TransferRequest transferRequest = new TransferRequest(receiver.getId(), new BigDecimal("10.00"));
        mockMvc.perform(post("/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", "fail-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @Test
    void getTransactions_returnsPaginatedHistory() throws Exception {
        String token = registerAndLogin("history@test.com", "password123");

        AddMoneyRequest addRequest = new AddMoneyRequest(new BigDecimal("25.00"));
        mockMvc.perform(post("/wallet/add")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "history-add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wallet/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void walletEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/wallet"))
                .andExpect(status().isForbidden());
    }
}
