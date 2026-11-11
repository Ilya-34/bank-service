package io.github.ilya34.bank.dto;

import io.github.ilya34.bank.entity.Currency;
import io.github.ilya34.bank.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(Long id, String idempotencyKey, Long fromAccountId, Long toAccountId,
                               BigDecimal amount, Currency currency, String description,
                               TransferStatus status, Instant createdAt) {
}
