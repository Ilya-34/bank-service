package io.github.ilya34.bank.service;

import io.github.ilya34.bank.entity.*;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.exception.ConflictException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.LedgerEntryRepository;
import io.github.ilya34.bank.repository.TransferRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional core of a transfer: debit + credit + transfer row + two ledger rows
 * either all commit or all roll back.
 */
@Service
public class TransferProcessor {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository ledger;

    public TransferProcessor(AccountRepository accounts, TransferRepository transfers, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
    }

    @Transactional
    public TransferResult execute(TransferCommand cmd) {
        if (cmd.fromAccountId().equals(cmd.toAccountId())) {
            throw new BusinessRuleException("SAME_ACCOUNT", "Source and destination accounts must differ");
        }
        Optional<TransferResult> replay = replayIfKnown(cmd);
        if (replay.isPresent()) {
            return replay.get();
        }

        // Lock both rows in ascending id order: two opposite transfers (A->B and B->A)
        // then queue on the same first lock instead of deadlocking.
        Long firstId = Math.min(cmd.fromAccountId(), cmd.toAccountId());
        Long secondId = Math.max(cmd.fromAccountId(), cmd.toAccountId());
        Account first = lock(firstId);
        Account second = lock(secondId);
        Account from = first.getId().equals(cmd.fromAccountId()) ? first : second;
        Account to = from == first ? second : first;

        // A concurrent request with the same key may have committed while we waited for the locks.
        replay = replayIfKnown(cmd);
        if (replay.isPresent()) {
            return replay.get();
        }

        if (!from.isActive()) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE",
                    "Source account " + from.getId() + " is " + from.getStatus());
        }
        if (!to.isActive()) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE",
                    "Destination account " + to.getId() + " is " + to.getStatus());
        }
        if (from.getCurrency() != to.getCurrency()) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Cross-currency transfers are not supported ("
                    + from.getCurrency() + " -> " + to.getCurrency() + ")");
        }

        from.debit(cmd.amount());
        to.credit(cmd.amount());

        Transfer transfer = transfers.saveAndFlush(new Transfer(cmd.idempotencyKey(), from.getId(), to.getId(),
                cmd.amount(), from.getCurrency(), cmd.description()));
        ledger.save(new LedgerEntry(from.getId(), transfer.getId(), EntryDirection.DEBIT, EntryOperation.TRANSFER,
                cmd.amount(), from.getBalance(), from.getCurrency()));
        ledger.save(new LedgerEntry(to.getId(), transfer.getId(), EntryDirection.CREDIT, EntryOperation.TRANSFER,
                cmd.amount(), to.getBalance(), to.getCurrency()));
        return new TransferResult(transfer, false);
    }

    @Transactional(readOnly = true)
    public Optional<TransferResult> findReplay(TransferCommand cmd) {
        return replayIfKnown(cmd);
    }

    private Optional<TransferResult> replayIfKnown(TransferCommand cmd) {
        return transfers.findByIdempotencyKey(cmd.idempotencyKey()).map(existing -> {
            if (!cmd.samePayloadAs(existing)) {
                throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key was already used with a different request");
            }
            return new TransferResult(existing, true);
        });
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Account", id));
    }
}
