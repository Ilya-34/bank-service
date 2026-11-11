package io.github.ilya34.bank.dto;

import io.github.ilya34.bank.entity.AccountStatus;
import io.github.ilya34.bank.entity.Currency;
import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(Long accountId, Currency currency, BigDecimal balance,
                              AccountStatus status, Instant asOf) {
}
