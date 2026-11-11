package io.github.ilya34.bank.service;

import io.github.ilya34.bank.entity.Account;
import io.github.ilya34.bank.entity.Card;
import io.github.ilya34.bank.entity.CardStatus;
import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.AccountRepository;
import io.github.ilya34.bank.repository.CardRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private static final int VALID_YEARS = 4;

    private final CardRepository cards;
    private final AccountRepository accounts;
    private final CardNumberGenerator generator;
    private final Clock clock;

    public CardService(CardRepository cards, AccountRepository accounts,
                       CardNumberGenerator generator, Clock clock) {
        this.cards = cards;
        this.accounts = accounts;
        this.generator = generator;
        this.clock = clock;
    }

    @Transactional
    public Card issue(Long accountId) {
        Account account = accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Account", accountId));
        if (!account.isActive()) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE",
                    "Cannot issue a card for account " + accountId + " in status " + account.getStatus());
        }
        String masked = generator.mask(generator.generate());
        LocalDate expires = YearMonth.now(clock).plusYears(VALID_YEARS).atEndOfMonth();
        return cards.save(new Card(accountId, masked, expires));
    }

    @Transactional(readOnly = true)
    public Card get(Long id) {
        return cards.findById(id).orElseThrow(() -> new NotFoundException("Card", id));
    }

    @Transactional(readOnly = true)
    public List<Card> listForAccount(Long accountId) {
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("Account", accountId);
        }
        return cards.findByAccountIdOrderById(accountId);
    }

    @Transactional
    public Card block(Long id) {
        Card card = get(id);
        card.setStatus(CardStatus.BLOCKED);
        return card;
    }

    @Transactional
    public Card unblock(Long id) {
        Card card = get(id);
        Account account = accounts.findById(card.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account", card.getAccountId()));
        if (!account.isActive()) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE",
                    "Cannot unblock a card while account " + account.getId() + " is " + account.getStatus());
        }
        if (card.getExpiresAt().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("CARD_EXPIRED", "Card " + id + " has expired");
        }
        card.setStatus(CardStatus.ACTIVE);
        return card;
    }
}
