package io.github.ilya34.bank.service;

import io.github.ilya34.bank.entity.Transfer;
import io.github.ilya34.bank.exception.BadRequestException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.TransferRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point for transfers. Deliberately NOT transactional: the transaction lives in
 * {@link TransferProcessor}, so a unique-key violation caused by a racing duplicate request
 * can be caught here after the failed transaction has rolled back, and answered with the
 * transfer that won the race.
 */
@Service
public class TransferService {

    private static final int MAX_KEY_LENGTH = 64;

    private final TransferProcessor processor;
    private final TransferRepository transfers;

    public TransferService(TransferProcessor processor, TransferRepository transfers) {
        this.processor = processor;
        this.transfers = transfers;
    }

    public TransferResult transfer(TransferCommand cmd) {
        String key = cmd.idempotencyKey();
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key header is required (1-" + MAX_KEY_LENGTH + " characters)");
        }
        try {
            return processor.execute(cmd);
        } catch (DataIntegrityViolationException e) {
            return processor.findReplay(cmd).orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public Transfer get(Long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFoundException("Transfer", id));
    }
}
