package io.github.ilya34.bank.repository;

import io.github.ilya34.bank.entity.Transfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
