package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ilya34.bank.entity.*;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.CustomerRepository;
import io.github.ilya34.bank.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accounts;
    @Mock CustomerRepository customers;
    @Mock LedgerEntryRepository ledger;
    @Mock AccountNumberGenerator numbers;

    private AccountService service() {
        return new AccountService(accounts, customers, ledger, numbers);
    }

    private static Account account(Currency currency, String balance) {
        Account a = new Account(1L, "BS00000000000001", currency);
        a.credit(new BigDecimal(balance));
        return a;
    }

    @Test
    void openRequiresExistingCustomer() {
        when(customers.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service().open(5L, Currency.USD)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void openCreatesActiveAccountWithZeroBalance() {
        when(customers.existsById(5L)).thenReturn(true);
        when(numbers.next()).thenReturn("BS12345678901234");
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account a = service().open(5L, Currency.EUR);

        assertThat(a.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(a.getBalance()).isEqualByComparingTo("0");
        assertThat(a.getCurrency()).isEqualTo(Currency.EUR);
    }

    @Test
    void closeRejectsNonZeroBalance() {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account(Currency.USD, "10.00")));

        assertThatThrownBy(() -> service().close(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ACCOUNT_NOT_EMPTY");
    }

    @Test
    void closeEmptyAccountSucceeds() {
        Account a = account(Currency.USD, "0.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));

        assertThat(service().close(1L).getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closedAccountCannotBeUnblocked() {
        Account a = account(Currency.USD, "0.00");
        a.setStatus(AccountStatus.CLOSED);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service().unblock(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void depositCreditsBalanceAndWritesLedgerEntry() {
        Account a = account(Currency.USD, "5.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));

        service().deposit(1L, new BigDecimal("20.25"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledger).save(captor.capture());
        assertThat(a.getBalance()).isEqualByComparingTo("25.25");
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo("25.25");
        assertThat(captor.getValue().getDirection()).isEqualTo(EntryDirection.CREDIT);
        assertThat(captor.getValue().getOperation()).isEqualTo(EntryOperation.DEPOSIT);
    }

    @Test
    void depositToBlockedAccountIsRejected() {
        Account a = account(Currency.USD, "0.00");
        a.setStatus(AccountStatus.BLOCKED);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service().deposit(1L, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ACCOUNT_NOT_ACTIVE");
    }
}
