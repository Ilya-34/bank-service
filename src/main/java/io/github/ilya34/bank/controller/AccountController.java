package io.github.ilya34.bank.controller;

import io.github.ilya34.bank.dto.*;
import io.github.ilya34.bank.entity.Account;
import io.github.ilya34.bank.mapper.Mappers;
import io.github.ilya34.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/customers/{customerId}/accounts")
    @Operation(summary = "Open an account for a customer (USD, EUR or BYN)")
    public ResponseEntity<AccountResponse> open(@PathVariable Long customerId,
                                                @Valid @RequestBody OpenAccountRequest request) {
        Account a = service.open(customerId, request.currency());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + a.getId())).body(Mappers.toResponse(a));
    }

    @GetMapping("/customers/{customerId}/accounts")
    @Operation(summary = "List a customer's accounts")
    public List<AccountResponse> listForCustomer(@PathVariable Long customerId) {
        return service.listForCustomer(customerId).stream().map(Mappers::toResponse).toList();
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get an account")
    public AccountResponse get(@PathVariable Long id) {
        return Mappers.toResponse(service.get(id));
    }

    @GetMapping("/accounts/{id}/balance")
    @Operation(summary = "Get the current balance")
    public BalanceResponse balance(@PathVariable Long id) {
        Account a = service.get(id);
        return new BalanceResponse(a.getId(), a.getCurrency(), a.getBalance(), a.getStatus(), Instant.now());
    }

    @PostMapping("/accounts/{id}/block")
    @Operation(summary = "Block an account (no transfers in or out)")
    public AccountResponse block(@PathVariable Long id) {
        return Mappers.toResponse(service.block(id));
    }

    @PostMapping("/accounts/{id}/unblock")
    @Operation(summary = "Unblock an account")
    public AccountResponse unblock(@PathVariable Long id) {
        return Mappers.toResponse(service.unblock(id));
    }

    @PostMapping("/accounts/{id}/close")
    @Operation(summary = "Close an account", description = "Only allowed when the balance is zero. Irreversible.")
    public AccountResponse close(@PathVariable Long id) {
        return Mappers.toResponse(service.close(id));
    }

    @PostMapping("/accounts/{id}/deposit")
    @Operation(summary = "Demo top-up", description = "Credits the account and writes a ledger entry. "
            + "Stands in for an external funding source so the demo can be tried end to end.")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return Mappers.toResponse(service.deposit(id, request.amount()));
    }

    @GetMapping("/accounts/{id}/transactions")
    @Operation(summary = "Ledger history of an account, newest first (paginated)")
    public PageResponse<LedgerEntryResponse> history(@PathVariable Long id,
                                                     @PageableDefault Pageable pageable) {
        return PageResponse.of(service.history(id, pageable), Mappers::toResponse);
    }
}
