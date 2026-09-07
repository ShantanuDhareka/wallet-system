package com.shantanu.wallet.dto;

import com.shantanu.wallet.entity.TransactionStatus;
import com.shantanu.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String idempotencyKey,
        LocalDateTime createdAt
) {
}