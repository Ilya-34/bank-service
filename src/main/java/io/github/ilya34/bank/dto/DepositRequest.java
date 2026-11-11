package io.github.ilya34.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DepositRequest(
        @Schema(example = "500.00", description = "Demo top-up; greater than zero, at most 2 decimal places")
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {
}
