package io.github.ilya34.bank.dto;

import io.github.ilya34.bank.entity.Currency;
import io.github.ilya34.bank.entity.EntryDirection;
import io.github.ilya34.bank.entity.EntryOperation;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(Long id, Long accountId, Long transferId, EntryDirection direction,
                                  EntryOperation operation, BigDecimal amount, BigDecimal balanceAfter,
                                  Currency currency, Instant createdAt) {
}
