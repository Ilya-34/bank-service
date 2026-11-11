package io.github.ilya34.bank.controller;

import io.github.ilya34.bank.dto.CardResponse;
import io.github.ilya34.bank.entity.Card;
import io.github.ilya34.bank.mapper.Mappers;
import io.github.ilya34.bank.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cards")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @PostMapping("/accounts/{accountId}/cards")
    @Operation(summary = "Issue a test card for an account",
            description = "Returns only the masked number. The generated full number is not stored.")
    public ResponseEntity<CardResponse> issue(@PathVariable Long accountId) {
        Card c = service.issue(accountId);
        return ResponseEntity.created(URI.create("/api/v1/cards/" + c.getId())).body(Mappers.toResponse(c));
    }

    @GetMapping("/accounts/{accountId}/cards")
    @Operation(summary = "List cards of an account")
    public List<CardResponse> list(@PathVariable Long accountId) {
        return service.listForAccount(accountId).stream().map(Mappers::toResponse).toList();
    }

    @GetMapping("/cards/{id}")
    @Operation(summary = "Get a card")
    public CardResponse get(@PathVariable Long id) {
        return Mappers.toResponse(service.get(id));
    }

    @PostMapping("/cards/{id}/block")
    @Operation(summary = "Block a card")
    public CardResponse block(@PathVariable Long id) {
        return Mappers.toResponse(service.block(id));
    }

    @PostMapping("/cards/{id}/unblock")
    @Operation(summary = "Unblock a card (account must be active)")
    public CardResponse unblock(@PathVariable Long id) {
        return Mappers.toResponse(service.unblock(id));
    }
}
