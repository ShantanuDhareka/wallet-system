package com.shantanu.wallet.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        BigDecimal balance
) {
}
