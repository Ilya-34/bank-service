package io.github.ilya34.bank.service;

import io.github.ilya34.bank.dto.CustomerRequest;
import io.github.ilya34.bank.entity.Customer;
import io.github.ilya34.bank.exception.ConflictException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.CustomerRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    public Customer create(CustomerRequest req) {
        String email = normalizeEmail(req.email());
        if (customers.existsByEmail(email)) {
            throw emailTaken(email);
        }
        return customers.save(new Customer(req.firstName().trim(), req.lastName().trim(), email, req.phone()));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Page<Customer> list(Pageable pageable) {
        return customers.findAll(pageable);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest req) {
        Customer customer = get(id);
        String email = normalizeEmail(req.email());
        if (customers.existsByEmailAndIdNot(email, id)) {
            throw emailTaken(email);
        }
        customer.update(req.firstName().trim(), req.lastName().trim(), email, req.phone());
        return customers.saveAndFlush(customer);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ConflictException emailTaken(String email) {
        return new ConflictException("EMAIL_ALREADY_EXISTS", "A customer with email " + email + " already exists");
    }
}
