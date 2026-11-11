package io.github.ilya34.bank.repository;

import io.github.ilya34.bank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
