package com.shantanu.wallet.controller;

import com.shantanu.wallet.dto.TransactionResponse;
import com.shantanu.wallet.dto.WalletResponse;
import com.shantanu.wallet.entity.Transaction;
import com.shantanu.wallet.entity.Wallet;
import com.shantanu.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final WalletService walletService;

    public AdminController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallets")
    public ResponseEntity<Page<WalletResponse>> getAllWallets(Pageable pageable) {
        Page<WalletResponse> wallets = walletService.getAllWallets(pageable)
                .map(wallet -> new WalletResponse(wallet.getId(), wallet.getBalance()));

        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(Pageable pageable) {
        Page<TransactionResponse> transactions = walletService.getAllTransactions(pageable)
                .map(this::toTransactionResponse);

        return ResponseEntity.ok(transactions);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromWallet() != null ? transaction.getFromWallet().getId() : null,
                transaction.getToWallet() != null ? transaction.getToWallet().getId() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        );
    }
}
