package io.github.ilya34.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private Long toAccountId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Column(updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.COMPLETED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public Transfer(String idempotencyKey, Long fromAccountId, Long toAccountId,
                    BigDecimal amount, Currency currency, String description) {
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public String getDescription() { return description; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
