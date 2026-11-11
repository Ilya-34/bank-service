package io.github.ilya34.bank.repository;

import io.github.ilya34.bank.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);

    /** Net of all entries for an account; used by tests to check the ledger against the balance. */
    @Query("select coalesce(sum(case when e.direction = io.github.ilya34.bank.entity.EntryDirection.CREDIT "
            + "then e.amount else -e.amount end), 0) from LedgerEntry e where e.accountId = :accountId")
    java.math.BigDecimal netForAccount(@Param("accountId") Long accountId);
}
