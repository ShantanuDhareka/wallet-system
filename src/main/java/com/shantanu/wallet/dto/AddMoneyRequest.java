package com.shantanu.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddMoneyRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount
) {
}