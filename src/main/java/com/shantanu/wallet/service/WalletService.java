package com.shantanu.wallet.service;

import com.shantanu.wallet.dto.AddMoneyRequest;
import com.shantanu.wallet.dto.TransferRequest;
import com.shantanu.wallet.entity.Transaction;
import com.shantanu.wallet.entity.TransactionStatus;
import com.shantanu.wallet.entity.TransactionType;
import com.shantanu.wallet.entity.Wallet;
import com.shantanu.wallet.exception.BadRequestException;
import com.shantanu.wallet.exception.InsufficientBalanceException;
import com.shantanu.wallet.exception.WalletNotFoundException;
import com.shantanu.wallet.repository.TransactionRepository;
import com.shantanu.wallet.repository.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction addMoney(
            String email,
            AddMoneyRequest request,
            String idempotencyKey) {

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> processAddMoney(email, request, idempotencyKey));
    }

    private Transaction processAddMoney(
            String email,
            AddMoneyRequest request,
            String idempotencyKey) {

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        Wallet lockedWallet = walletRepository.findByIdForUpdate(wallet.getId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        lockedWallet.setBalance(lockedWallet.getBalance().add(request.amount()));
        walletRepository.save(lockedWallet);

        Transaction transaction = buildTransaction(
                null,
                lockedWallet,
                request.amount(),
                TransactionType.ADD,
                idempotencyKey
        );

        try {
            return transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public Transaction transfer(
            String senderEmail,
            TransferRequest request,
            String idempotencyKey) {

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> processTransfer(senderEmail, request, idempotencyKey));
    }

    private Transaction processTransfer(
            String senderEmail,
            TransferRequest request,
            String idempotencyKey) {

        Wallet sender = walletRepository.findByUserEmail(senderEmail)
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));

        Wallet receiver = walletRepository.findByUserId(request.toUserId())
                .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Cannot transfer to your own wallet");
        }

        Long firstId = Math.min(sender.getId(), receiver.getId());
        Long secondId = Math.max(sender.getId(), receiver.getId());

        Wallet firstLocked = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        Wallet secondLocked = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        Wallet lockedSender = sender.getId().equals(firstId) ? firstLocked : secondLocked;
        Wallet lockedReceiver = receiver.getId().equals(firstId) ? firstLocked : secondLocked;

        if (lockedSender.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        lockedSender.setBalance(lockedSender.getBalance().subtract(request.amount()));
        lockedReceiver.setBalance(lockedReceiver.getBalance().add(request.amount()));

        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);

        Transaction transaction = buildTransaction(
                lockedSender,
                lockedReceiver,
                request.amount(),
                TransactionType.TRANSFER,
                idempotencyKey
        );

        try {
            return transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    public Wallet getWallet(String email) {
        return walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }

    public Page<Transaction> getTransactions(String email, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        return transactionRepository.findByWalletId(wallet.getId(), pageable);
    }

    public Page<Wallet> getAllWallets(Pageable pageable) {
        return walletRepository.findAll(pageable);
    }

    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    private Transaction buildTransaction(
            Wallet fromWallet,
            Wallet toWallet,
            java.math.BigDecimal amount,
            TransactionType type,
            String idempotencyKey) {

        Transaction transaction = new Transaction();
        transaction.setFromWallet(fromWallet);
        transaction.setToWallet(toWallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        return transaction;
    }
}
