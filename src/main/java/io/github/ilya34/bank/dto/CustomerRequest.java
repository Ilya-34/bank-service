package io.github.ilya34.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @Schema(example = "Anna") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Ivanova") @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "anna.ivanova@example.com") @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "+375291234567", description = "Digits with an optional leading +, 7 to 15 digits")
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be 7-15 digits with an optional leading +")
        String phone) {
}
