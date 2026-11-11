package io.github.ilya34.bank.service;

import io.github.ilya34.bank.entity.*;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.CustomerRepository;
import io.github.ilya34.bank.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final LedgerEntryRepository ledger;
    private final AccountNumberGenerator numbers;

    public AccountService(AccountRepository accounts, CustomerRepository customers,
                          LedgerEntryRepository ledger, AccountNumberGenerator numbers) {
        this.accounts = accounts;
        this.customers = customers;
        this.ledger = ledger;
        this.numbers = numbers;
    }

    @Transactional
    public Account open(Long customerId, Currency currency) {
        if (!customers.existsById(customerId)) {
            throw new NotFoundException("Customer", customerId);
        }
        return accounts.save(new Account(customerId, numbers.next(), currency));
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
    }

    @Transactional(readOnly = true)
    public List<Account> listForCustomer(Long customerId) {
        if (!customers.existsById(customerId)) {
            throw new NotFoundException("Customer", customerId);
        }
        return accounts.findByCustomerIdOrderById(customerId);
    }

    @Transactional
    public Account block(Long id) {
        Account account = lock(id);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("ACCOUNT_CLOSED", "Account " + id + " is closed");
        }
        account.setStatus(AccountStatus.BLOCKED);
        return account;
    }

    @Transactional
    public Account unblock(Long id) {
        Account account = lock(id);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("ACCOUNT_CLOSED", "Account " + id + " is closed");
        }
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    @Transactional
    public Account close(Long id) {
        Account account = lock(id);
        if (account.getBalance().signum() != 0) {
            throw new BusinessRuleException("ACCOUNT_NOT_EMPTY",
                    "Account " + id + " has a non-zero balance and cannot be closed");
        }
        account.setStatus(AccountStatus.CLOSED);
        return account;
    }

    /** Demo top-up so that accounts can be funded without an external payment system. */
    @Transactional
    public Account deposit(Long id, BigDecimal amount) {
        Account account = lock(id);
        if (!account.isActive()) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE", "Account " + id + " is " + account.getStatus());
        }
        account.credit(amount);
        ledger.save(new LedgerEntry(id, null, EntryDirection.CREDIT, EntryOperation.DEPOSIT,
                amount, account.getBalance(), account.getCurrency()));
        return account;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> history(Long accountId, Pageable pageable) {
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("Account", accountId);
        }
        return ledger.findByAccountIdOrderByIdDesc(accountId, pageable);
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Account", id));
    }
}
