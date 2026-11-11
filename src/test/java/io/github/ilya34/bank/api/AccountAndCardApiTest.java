package io.github.ilya34.bank.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AccountAndCardApiTest extends ApiTestBase {

    @Test
    void openAccountStartsActiveWithZeroBalance() throws Exception {
        long customer = createCustomer();

        mvc.perform(post("/api/v1/customers/{id}/accounts", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"BYN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("BYN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("BS\\d{14}")));
    }

    @Test
    void unsupportedCurrencyReturns400() throws Exception {
        long customer = createCustomer();

        mvc.perform(post("/api/v1/customers/{id}/accounts", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"XYZ\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openAccountForUnknownCustomerReturns404() throws Exception {
        mvc.perform(post("/api/v1/customers/{id}/accounts", 987_654).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void depositUpdatesBalanceAndLedger() throws Exception {
        long account = fundedAccount("USD", "125.50");

        mvc.perform(get("/api/v1/accounts/{id}/balance", account))
                .andExpect(jsonPath("$.balance").value(125.5));
        mvc.perform(get("/api/v1/accounts/{id}/transactions", account))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].operation").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(125.5));
    }

    @Test
    void depositRejectsZeroNegativeAndThreeDecimals() throws Exception {
        long account = fundedAccount("USD", "0");
        for (String bad : new String[]{"0", "-5", "1.005"}) {
            mvc.perform(post("/api/v1/accounts/{id}/deposit", account).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":" + bad + "}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void blockedAccountRejectsDepositAndCanBeUnblocked() throws Exception {
        long account = fundedAccount("EUR", "10");

        mvc.perform(post("/api/v1/accounts/{id}/block", account))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(post("/api/v1/accounts/{id}/deposit", account).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        mvc.perform(post("/api/v1/accounts/{id}/unblock", account))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void closeRequiresZeroBalance() throws Exception {
        long funded = fundedAccount("USD", "5");
        long empty = fundedAccount("USD", "0");

        mvc.perform(post("/api/v1/accounts/{id}/close", funded))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_EMPTY"));
        mvc.perform(post("/api/v1/accounts/{id}/close", empty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void issuedCardIsMaskedAndCanBeBlockedAndUnblocked() throws Exception {
        long account = fundedAccount("USD", "0");

        long cardId = body(mvc.perform(post("/api/v1/accounts/{id}/cards", account))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedNumber").value(org.hamcrest.Matchers.matchesPattern(
                        "\\*\\*\\*\\* \\*\\*\\*\\* \\*\\*\\*\\* \\d{4}")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").exists())).get("id").asLong();

        mvc.perform(post("/api/v1/cards/{id}/block", cardId))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(post("/api/v1/cards/{id}/unblock", cardId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/v1/accounts/{id}/cards", account)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void cardCannotBeIssuedForBlockedAccount() throws Exception {
        long account = fundedAccount("USD", "0");
        mvc.perform(post("/api/v1/accounts/{id}/block", account)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/accounts/{id}/cards", account))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownCardReturns404() throws Exception {
        mvc.perform(get("/api/v1/cards/{id}", 424_242)).andExpect(status().isNotFound());
    }
}
