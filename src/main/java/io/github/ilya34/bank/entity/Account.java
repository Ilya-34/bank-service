package io.github.ilya34.bank.entity;

import io.github.ilya34.bank.exception.BusinessRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "account_number", nullable = false, updatable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** Optimistic lock: a safety net on top of the pessimistic row locks used for money movement. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(Long customerId, String accountNumber, Currency currency) {
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS",
                    "Insufficient funds on account " + id + ": balance " + balance + ", requested " + amount);
        }
        balance = balance.subtract(amount);
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
