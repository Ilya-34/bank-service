package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.ilya34.bank.entity.*;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.CardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);

    @Mock CardRepository cards;
    @Mock AccountRepository accounts;

    private CardService service() {
        return new CardService(cards, accounts, new CardNumberGenerator(), CLOCK);
    }

    @Test
    void issueStoresMaskedNumberAndExpiryFourYearsAhead() {
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account(1L, "BS1", Currency.USD)));
        when(cards.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));

        Card card = service().issue(1L);

        assertThat(card.getMaskedNumber()).matches("\\*\\*\\*\\* \\*\\*\\*\\* \\*\\*\\*\\* \\d{4}");
        assertThat(card.getExpiresAt()).isEqualTo(LocalDate.of(2030, 3, 31));
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void issueRejectsBlockedAccount() {
        Account a = new Account(1L, "BS1", Currency.USD);
        a.setStatus(AccountStatus.BLOCKED);
        when(accounts.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service().issue(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void blockThenUnblockRestoresActive() {
        Card card = new Card(1L, "**** **** **** 1111", LocalDate.of(2030, 1, 31));
        when(cards.findById(9L)).thenReturn(Optional.of(card));
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account(1L, "BS1", Currency.USD)));

        assertThat(service().block(9L).getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(service().unblock(9L).getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void unblockIsRejectedWhenAccountIsBlocked() {
        Card card = new Card(1L, "**** **** **** 1111", LocalDate.of(2030, 1, 31));
        card.setStatus(CardStatus.BLOCKED);
        Account a = new Account(1L, "BS1", Currency.USD);
        a.setStatus(AccountStatus.BLOCKED);
        when(cards.findById(9L)).thenReturn(Optional.of(card));
        when(accounts.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service().unblock(9L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unblockIsRejectedForExpiredCard() {
        Card card = new Card(1L, "**** **** **** 1111", LocalDate.of(2025, 1, 31));
        card.setStatus(CardStatus.BLOCKED);
        when(cards.findById(9L)).thenReturn(Optional.of(card));
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account(1L, "BS1", Currency.USD)));

        assertThatThrownBy(() -> service().unblock(9L))
                .hasFieldOrPropertyWithValue("code", "CARD_EXPIRED");
    }
}
