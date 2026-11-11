package io.github.ilya34.bank.mapper;

import io.github.ilya34.bank.dto.*;
import io.github.ilya34.bank.entity.*;

/** Hand-written entity -> DTO mapping. */
public final class Mappers {

    private Mappers() {
    }

    public static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    public static AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getCustomerId(), a.getAccountNumber(), a.getCurrency(),
                a.getBalance(), a.getStatus(), a.getCreatedAt());
    }

    public static CardResponse toResponse(Card c) {
        return new CardResponse(c.getId(), c.getAccountId(), c.getMaskedNumber(), c.getExpiresAt(),
                c.getStatus(), c.getCreatedAt());
    }

    public static TransferResponse toResponse(Transfer t) {
        return new TransferResponse(t.getId(), t.getIdempotencyKey(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getCurrency(), t.getDescription(), t.getStatus(), t.getCreatedAt());
    }

    public static LedgerEntryResponse toResponse(LedgerEntry e) {
        return new LedgerEntryResponse(e.getId(), e.getAccountId(), e.getTransferId(), e.getDirection(),
                e.getOperation(), e.getAmount(), e.getBalanceAfter(), e.getCurrency(), e.getCreatedAt());
    }
}
