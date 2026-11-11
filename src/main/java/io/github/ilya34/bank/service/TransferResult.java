package io.github.ilya34.bank.service;

import io.github.ilya34.bank.entity.Transfer;

/** replayed = true when the Idempotency-Key was already used and the stored transfer is returned. */
public record TransferResult(Transfer transfer, boolean replayed) {
}
