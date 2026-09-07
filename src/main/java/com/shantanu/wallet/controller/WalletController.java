package com.shantanu.wallet.controller;

import com.shantanu.wallet.dto.AddMoneyRequest;
import com.shantanu.wallet.dto.TransactionResponse;
import com.shantanu.wallet.dto.TransferRequest;
import com.shantanu.wallet.dto.WalletResponse;
import com.shantanu.wallet.entity.Transaction;
import com.shantanu.wallet.entity.Wallet;
import com.shantanu.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/add")
    public ResponseEntity<TransactionResponse> addMoney(
            @Valid @RequestBody AddMoneyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        Transaction transaction = walletService.addMoney(
                authentication.getName(),
                request,
                idempotencyKey
        );

        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        Transaction transaction = walletService.transfer(
                authentication.getName(),
                request,
                idempotencyKey
        );

        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(Authentication authentication) {
        Wallet wallet = walletService.getWallet(authentication.getName());

        return ResponseEntity.ok(new WalletResponse(
                wallet.getId(),
                wallet.getBalance()
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            Authentication authentication,
            Pageable pageable) {

        Page<TransactionResponse> transactions = walletService
                .getTransactions(authentication.getName(), pageable)
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
