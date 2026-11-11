package io.github.ilya34.bank.repository;

import io.github.ilya34.bank.entity.Card;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByAccountIdOrderById(Long accountId);
}
