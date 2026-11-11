package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ilya34.bank.dto.CustomerRequest;
import io.github.ilya34.bank.entity.Customer;
import io.github.ilya34.bank.exception.ConflictException;
import io.github.ilya34.bank.exception.NotFoundException;
import io.github.ilya34.bank.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customers;
    @InjectMocks CustomerService service;

    private static CustomerRequest request(String email) {
        return new CustomerRequest(" Anna ", "Ivanova", email, "+375291234567");
    }

    @Test
    void createNormalizesEmailAndTrimsNames() {
        when(customers.existsByEmail("anna@example.com")).thenReturn(false);
        when(customers.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer created = service.create(request("  Anna@Example.COM "));

        assertThat(created.getEmail()).isEqualTo("anna@example.com");
        assertThat(created.getFirstName()).isEqualTo("Anna");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(customers.existsByEmail("anna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("anna@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "EMAIL_ALREADY_EXISTS");
        verify(customers, never()).save(any());
    }

    @Test
    void getThrowsNotFound() {
        when(customers.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsEmailOwnedByAnotherCustomer() {
        when(customers.findById(1L)).thenReturn(Optional.of(new Customer("A", "B", "a@b.com", "+1234567")));
        when(customers.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("taken@example.com")))
                .isInstanceOf(ConflictException.class);
    }
}
