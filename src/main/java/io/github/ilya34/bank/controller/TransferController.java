package io.github.ilya34.bank.controller;

import io.github.ilya34.bank.dto.TransferRequest;
import io.github.ilya34.bank.dto.TransferResponse;
import io.github.ilya34.bank.mapper.Mappers;
import io.github.ilya34.bank.service.TransferCommand;
import io.github.ilya34.bank.service.TransferResult;
import io.github.ilya34.bank.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Transfer money between two accounts",
            description = "Same currency only. Requires an `Idempotency-Key` header: repeating the request with the "
                    + "same key returns the original transfer (200, `Idempotent-Replayed: true`) and moves no money. "
                    + "The same key with a different body gives 409.")
    @ApiResponse(responseCode = "201", description = "Transfer created")
    @ApiResponse(responseCode = "200", description = "Replay of an earlier request with the same key")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "409", description = "Idempotency key reused with a different request")
    @ApiResponse(responseCode = "422", description = "Insufficient funds, currency mismatch, same account, blocked account")
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "Client-generated unique key, up to 64 chars (e.g. a UUID)", example = "7b0f2c1e-3a54-4a0e-9a53-2f6a4f6f9d10")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        TransferResult result = service.transfer(new TransferCommand(idempotencyKey.trim(),
                request.fromAccountId(), request.toAccountId(), request.amount(), request.description()));
        TransferResponse body = Mappers.toResponse(result.transfer());
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transfer")
    public TransferResponse get(@PathVariable Long id) {
        return Mappers.toResponse(service.get(id));
    }
}
