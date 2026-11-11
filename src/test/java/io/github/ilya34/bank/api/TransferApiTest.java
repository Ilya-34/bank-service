package io.github.ilya34.bank.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.ilya34.bank.repository.TransferRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransferApiTest extends ApiTestBase {

    @Autowired TransferRepository transfers;

    private static String key() {
        return UUID.randomUUID().toString();
    }

    @Test
    void successfulTransferMovesMoneyAndWritesLedgerOnBothSides() throws Exception {
        long a = fundedAccount("USD", "100.00");
        long b = fundedAccount("USD", "0");

        transfer(key(), a, b, "40.25")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(40.25));

        assertThat(balance(a)).isEqualByComparingTo("59.75");
        assertThat(balance(b)).isEqualByComparingTo("40.25");
        mvc.perform(get("/api/v1/accounts/{id}/transactions", a))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))     // newest first
                .andExpect(jsonPath("$.content[0].balanceAfter").value(59.75));
        mvc.perform(get("/api/v1/accounts/{id}/transactions", b))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"));
    }

    @Test
    void insufficientFundsReturns422AndLeavesBalancesUntouched() throws Exception {
        long a = fundedAccount("USD", "10.00");
        long b = fundedAccount("USD", "0");

        transfer(key(), a, b, "10.01")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        assertThat(balance(a)).isEqualByComparingTo("10.00");
        assertThat(balance(b)).isEqualByComparingTo("0");
    }

    @Test
    void sameKeyReplaysOriginalResultAndMovesMoneyOnce() throws Exception {
        long a = fundedAccount("USD", "100.00");
        long b = fundedAccount("USD", "0");
        String key = key();

        long first = body(transfer(key, a, b, "30.00").andExpect(status().isCreated())).get("id").asLong();
        long second = body(transfer(key, a, b, "30.00")
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))).get("id").asLong();

        assertThat(second).isEqualTo(first);
        assertThat(balance(a)).isEqualByComparingTo("70.00");
        assertThat(balance(b)).isEqualByComparingTo("30.00");
        assertThat(transfers.findByIdempotencyKey(key)).isPresent();
    }

    @Test
    void sameKeyWithDifferentAmountReturns409() throws Exception {
        long a = fundedAccount("USD", "100.00");
        long b = fundedAccount("USD", "0");
        String key = key();
        transfer(key, a, b, "30.00").andExpect(status().isCreated());

        transfer(key, a, b, "31.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(balance(a)).isEqualByComparingTo("70.00");
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        long a = fundedAccount("USD", "10");
        long b = fundedAccount("USD", "0");

        mvc.perform(post("/api/v1/transfers").contentType("application/json")
                        .content("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1}".formatted(a, b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferToSameAccountReturns422() throws Exception {
        long a = fundedAccount("USD", "10");

        transfer(key(), a, a, "1.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"));
    }

    @Test
    void crossCurrencyTransferReturns422() throws Exception {
        long usd = fundedAccount("USD", "10");
        long eur = fundedAccount("EUR", "0");

        transfer(key(), usd, eur, "1.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
    }

    @Test
    void nonPositiveAndOverPreciseAmountsReturn400() throws Exception {
        long a = fundedAccount("USD", "10");
        long b = fundedAccount("USD", "0");

        for (String bad : new String[]{"0", "-1", "0.001", "\"abc\""}) {
            transfer(key(), a, b, bad).andExpect(status().isBadRequest());
        }
        assertThat(balance(a)).isEqualByComparingTo("10");
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        long a = fundedAccount("USD", "10");

        transfer(key(), a, 9_999_999L, "1.00").andExpect(status().isNotFound());
    }

    @Test
    void blockedAccountCannotSendOrReceive() throws Exception {
        long a = fundedAccount("USD", "10");
        long b = fundedAccount("USD", "10");
        mvc.perform(post("/api/v1/accounts/{id}/block", b)).andExpect(status().isOk());

        transfer(key(), a, b, "1.00").andExpect(status().isUnprocessableEntity());
        transfer(key(), b, a, "1.00").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getTransferById() throws Exception {
        long a = fundedAccount("USD", "10");
        long b = fundedAccount("USD", "0");
        long id = body(transfer(key(), a, b, "2.00")).get("id").asLong();

        mvc.perform(get("/api/v1/transfers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccountId").value(a));
        mvc.perform(get("/api/v1/transfers/{id}", 8_888_888)).andExpect(status().isNotFound());
    }

    @Test
    void ledgerHistoryIsPaginated() throws Exception {
        long a = fundedAccount("USD", "100");
        long b = fundedAccount("USD", "0");
        for (int i = 0; i < 4; i++) {
            transfer(key(), a, b, "1.00").andExpect(status().isCreated());
        }

        mvc.perform(get("/api/v1/accounts/{id}/transactions", a).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))   // 1 deposit + 4 debits
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void openApiDocsAndSwaggerUiAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/transfers']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
