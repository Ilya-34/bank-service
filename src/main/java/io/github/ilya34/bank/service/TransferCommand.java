package io.github.ilya34.bank.service;

import java.math.BigDecimal;

public record TransferCommand(String idempotencyKey, Long fromAccountId, Long toAccountId,
                              BigDecimal amount, String description) {

    boolean samePayloadAs(io.github.ilya34.bank.entity.Transfer t) {
        return t.getFromAccountId().equals(fromAccountId)
                && t.getToAccountId().equals(toAccountId)
                && t.getAmount().compareTo(amount) == 0;
    }
}
