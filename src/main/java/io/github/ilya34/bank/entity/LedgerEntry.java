package io.github.ilya34.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Immutable record of one balance change. */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "transfer_id", updatable = false)
    private Long transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EntryDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EntryOperation operation;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(Long accountId, Long transferId, EntryDirection direction, EntryOperation operation,
                       BigDecimal amount, BigDecimal balanceAfter, Currency currency) {
        this.accountId = accountId;
        this.transferId = transferId;
        this.direction = direction;
        this.operation = operation;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Long getTransferId() { return transferId; }
    public EntryDirection getDirection() { return direction; }
    public EntryOperation getOperation() { return operation; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Currency getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
