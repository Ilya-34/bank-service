package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.ilya34.bank.entity.Currency;
import io.github.ilya34.bank.entity.Transfer;
import io.github.ilya34.bank.exception.BadRequestException;
import io.github.ilya34.bank.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class TransferServiceTest {

    private final TransferProcessor processor = mock(TransferProcessor.class);
    private final TransferService service = new TransferService(processor, mock(TransferRepository.class));

    private static TransferCommand cmd(String key) {
        return new TransferCommand(key, 1L, 2L, new BigDecimal("5.00"), null);
    }

    @Test
    void blankKeyIsRejected() {
        assertThatThrownBy(() -> service.transfer(cmd("  "))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void tooLongKeyIsRejected() {
        assertThatThrownBy(() -> service.transfer(cmd("x".repeat(65)))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void uniqueKeyViolationFromRacingRequestReturnsTheWinningTransfer() {
        TransferCommand command = cmd("k");
        Transfer winner = new Transfer("k", 1L, 2L, new BigDecimal("5.00"), Currency.USD, null);
        when(processor.execute(command)).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(processor.findReplay(command)).thenReturn(Optional.of(new TransferResult(winner, true)));

        TransferResult result = service.transfer(command);

        assertThat(result.replayed()).isTrue();
        assertThat(result.transfer()).isSameAs(winner);
    }

    @Test
    void integrityViolationWithoutExistingTransferIsRethrown() {
        TransferCommand command = cmd("k");
        when(processor.execute(command)).thenThrow(new DataIntegrityViolationException("boom"));
        when(processor.findReplay(command)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(command)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
