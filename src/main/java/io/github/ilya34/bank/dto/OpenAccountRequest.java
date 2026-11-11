package io.github.ilya34.bank.dto;

import io.github.ilya34.bank.entity.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@Schema(example = "USD") @NotNull Currency currency) {
}
