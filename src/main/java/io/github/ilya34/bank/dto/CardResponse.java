package io.github.ilya34.bank.dto;

import io.github.ilya34.bank.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

public record CardResponse(Long id, Long accountId,
                           @Schema(example = "**** **** **** 1234") String maskedNumber,
                           LocalDate expiresAt, CardStatus status, Instant createdAt) {
}
