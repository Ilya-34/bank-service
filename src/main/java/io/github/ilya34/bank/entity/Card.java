package io.github.ilya34.bank.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Only the masked number is stored. The full generated test number is never persisted. */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "masked_number", nullable = false, updatable = false)
    private String maskedNumber;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.ACTIVE;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Card() {
    }

    public Card(Long accountId, String maskedNumber, LocalDate expiresAt) {
        this.accountId = accountId;
        this.maskedNumber = maskedNumber;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getMaskedNumber() { return maskedNumber; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public CardStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
