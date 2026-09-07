package com.shantanu.wallet.repository;

import com.shantanu.wallet.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.fromWallet.id = :walletId OR t.toWallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletId(@Param("walletId") Long walletId, Pageable pageable);
}
