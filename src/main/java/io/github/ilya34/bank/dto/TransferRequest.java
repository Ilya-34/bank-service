package io.github.ilya34.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransferRequest(
        @Schema(example = "1") @NotNull Long fromAccountId,
        @Schema(example = "2") @NotNull Long toAccountId,
        @Schema(example = "25.50") @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Schema(example = "Rent for March") @Size(max = 140) String description) {
}
