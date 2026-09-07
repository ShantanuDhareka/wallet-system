package com.shantanu.wallet.service;

import com.shantanu.wallet.dto.AddMoneyRequest;
import com.shantanu.wallet.dto.TransferRequest;
import com.shantanu.wallet.entity.*;
import com.shantanu.wallet.exception.BadRequestException;
import com.shantanu.wallet.exception.InsufficientBalanceException;
import com.shantanu.wallet.exception.WalletNotFoundException;
import com.shantanu.wallet.repository.TransactionRepository;
import com.shantanu.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        wallet = new Wallet();
        wallet.setId(10L);
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setVersion(0L);
    }

    @Test
    void addMoney_createsTransaction() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail("user@test.com")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        Transaction result = walletService.addMoney(
                "user@test.com",
                new AddMoneyRequest(new BigDecimal("50.00")),
                "key-1"
        );

        assertThat(result.getType()).isEqualTo(TransactionType.ADD);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void addMoney_returnsExistingTransactionForDuplicateKey() {
        Transaction existing = new Transaction();
        existing.setId(99L);
        existing.setIdempotencyKey("dup-key");

        when(transactionRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        Transaction result = walletService.addMoney(
                "user@test.com",
                new AddMoneyRequest(new BigDecimal("10.00")),
                "dup-key"
        );

        assertThat(result.getId()).isEqualTo(99L);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transfer_throwsWhenInsufficientBalance() {
        wallet.setBalance(new BigDecimal("5.00"));
        Wallet receiver = new Wallet();
        receiver.setId(20L);
        User receiverUser = new User();
        receiverUser.setId(2L);
        receiver.setUser(receiverUser);

        when(transactionRepository.findByIdempotencyKey("t-key")).thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail("user@test.com")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> walletService.transfer(
                "user@test.com",
                new TransferRequest(2L, new BigDecimal("10.00")),
                "t-key"
        )).isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void transfer_throwsWhenSelfTransfer() {
        when(transactionRepository.findByIdempotencyKey("self-key")).thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail("user@test.com")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.transfer(
                "user@test.com",
                new TransferRequest(1L, new BigDecimal("10.00")),
                "self-key"
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getWallet_throwsWhenNotFound() {
        when(walletRepository.findByUserEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet("missing@test.com"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getTransactions_returnsPagedResults() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAmount(new BigDecimal("10.00"));
        tx.setType(TransactionType.ADD);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setIdempotencyKey("k1");
        tx.setCreatedAt(LocalDateTime.now());
        tx.setToWallet(wallet);

        when(walletRepository.findByUserEmail("user@test.com")).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<Transaction> page = walletService.getTransactions("user@test.com", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
