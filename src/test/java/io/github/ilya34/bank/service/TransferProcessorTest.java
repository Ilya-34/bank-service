package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.ilya34.bank.entity.*;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.exception.ConflictException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.LedgerEntryRepository;
import io.github.ilya34.bank.repository.TransferRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferProcessorTest {

    @Mock AccountRepository accounts;
    @Mock TransferRepository transfers;
    @Mock LedgerEntryRepository ledger;

    TransferProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransferProcessor(accounts, transfers, ledger);
    }

    private static Account account(long id, Currency currency, String balance) {
        Account a = new Account(100L, "BS" + id, currency);
        setId(a, id);
        a.credit(new BigDecimal(balance));
        return a;
    }

    private static void setId(Object entity, long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TransferCommand cmd(long from, long to, String amount) {
        return new TransferCommand("key-1", from, to, new BigDecimal(amount), "test");
    }

    private void stubSave() {
        when(transfers.saveAndFlush(any(Transfer.class))).thenAnswer(i -> {
            Transfer t = i.getArgument(0);
            setId(t, 55L);
            return t;
        });
    }

    @Test
    void movesMoneyAndWritesTwoLedgerEntries() {
        Account a = account(1, Currency.USD, "100.00");
        Account b = account(2, Currency.USD, "5.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));
        stubSave();

        TransferResult result = processor.execute(cmd(1, 2, "30.50"));

        assertThat(result.replayed()).isFalse();
        assertThat(a.getBalance()).isEqualByComparingTo("69.50");
        assertThat(b.getBalance()).isEqualByComparingTo("35.50");
        ArgumentCaptor<LedgerEntry> entries = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledger, times(2)).save(entries.capture());
        assertThat(entries.getAllValues()).extracting(LedgerEntry::getDirection)
                .containsExactly(EntryDirection.DEBIT, EntryDirection.CREDIT);
        assertThat(entries.getAllValues()).extracting(LedgerEntry::getTransferId).containsOnly(55L);
    }

    @Test
    void locksAccountsInAscendingIdOrderRegardlessOfDirection() {
        Account low = account(1, Currency.USD, "10.00");
        Account high = account(9, Currency.USD, "100.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(low));
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(high));
        stubSave();

        processor.execute(cmd(9, 1, "10.00"));   // from the HIGHER id to the lower one

        InOrder order = inOrder(accounts);
        order.verify(accounts).findByIdForUpdate(1L);
        order.verify(accounts).findByIdForUpdate(9L);
        assertThat(high.getBalance()).isEqualByComparingTo("90.00");
        assertThat(low.getBalance()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertThatThrownBy(() -> processor.execute(cmd(1, 1, "1.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "SAME_ACCOUNT");
        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsInsufficientFundsAndChangesNothing() {
        Account a = account(1, Currency.USD, "10.00");
        Account b = account(2, Currency.USD, "0.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> processor.execute(cmd(1, 2, "10.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INSUFFICIENT_FUNDS");
        assertThat(a.getBalance()).isEqualByComparingTo("10.00");
        assertThat(b.getBalance()).isEqualByComparingTo("0.00");
        verify(transfers, never()).saveAndFlush(any());
    }

    @Test
    void allowsTransferOfEntireBalance() {
        Account a = account(1, Currency.USD, "10.00");
        Account b = account(2, Currency.USD, "0.00");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));
        stubSave();

        processor.execute(cmd(1, 2, "10.00"));

        assertThat(a.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsDifferentCurrencies() {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account(1, Currency.USD, "10.00")));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(account(2, Currency.EUR, "10.00")));

        assertThatThrownBy(() -> processor.execute(cmd(1, 2, "1.00")))
                .hasFieldOrPropertyWithValue("code", "CURRENCY_MISMATCH");
    }

    @Test
    void rejectsBlockedDestination() {
        Account b = account(2, Currency.USD, "0.00");
        b.setStatus(AccountStatus.BLOCKED);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account(1, Currency.USD, "10.00")));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> processor.execute(cmd(1, 2, "1.00")))
                .hasFieldOrPropertyWithValue("code", "ACCOUNT_NOT_ACTIVE");
    }

    @Test
    void unknownAccountIsNotFound() {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.execute(cmd(1, 2, "1.00"))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void sameKeyAndPayloadReturnsStoredTransferWithoutTouchingBalances() {
        Transfer existing = new Transfer("key-1", 1L, 2L, new BigDecimal("30.50"), Currency.USD, "test");
        when(transfers.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        TransferResult result = processor.execute(cmd(1, 2, "30.5"));   // 30.5 == 30.50

        assertThat(result.replayed()).isTrue();
        assertThat(result.transfer()).isSameAs(existing);
        verifyNoInteractions(accounts, ledger);
    }

    @Test
    void sameKeyWithDifferentAmountIsConflict() {
        Transfer existing = new Transfer("key-1", 1L, 2L, new BigDecimal("30.50"), Currency.USD, "test");
        when(transfers.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> processor.execute(cmd(1, 2, "31.00")))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "IDEMPOTENCY_KEY_REUSED");
    }
}
