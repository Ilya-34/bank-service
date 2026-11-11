package io.github.ilya34.bank.controller;

import io.github.ilya34.bank.dto.CustomerRequest;
import io.github.ilya34.bank.dto.CustomerResponse;
import io.github.ilya34.bank.dto.PageResponse;
import io.github.ilya34.bank.entity.Customer;
import io.github.ilya34.bank.mapper.Mappers;
import io.github.ilya34.bank.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a customer", description = "409 if the email is already registered.")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer c = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + c.getId())).body(Mappers.toResponse(c));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    public CustomerResponse get(@PathVariable Long id) {
        return Mappers.toResponse(service.get(id));
    }

    @GetMapping
    @Operation(summary = "List customers (paginated, `page` is 0-based, `size` max 100)")
    public PageResponse<CustomerResponse> list(@PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(service.list(pageable), Mappers::toResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace customer data")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return Mappers.toResponse(service.update(id, request));
    }
}
