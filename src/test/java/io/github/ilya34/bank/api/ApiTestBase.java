package io.github.ilya34.bank.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Shared helpers: tests talk to the app only through HTTP (MockMvc) against in-memory H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected long createCustomer() throws Exception {
        String email = "user" + SEQ.incrementAndGet() + "-" + UUID.randomUUID() + "@example.com";
        String payload = """
                {"firstName":"Test","lastName":"User","email":"%s","phone":"+375291234567"}""".formatted(email);
        return body(mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    protected long openAccount(long customerId, String currency) throws Exception {
        return body(mvc.perform(post("/api/v1/customers/{id}/accounts", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"" + currency + "\"}"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    /** New customer + account in the given currency, funded with the given amount. */
    protected long fundedAccount(String currency, String amount) throws Exception {
        long id = openAccount(createCustomer(), currency);
        if (!"0".equals(amount)) {
            deposit(id, amount);
        }
        return id;
    }

    protected void deposit(long accountId, String amount) throws Exception {
        mvc.perform(post("/api/v1/accounts/{id}/deposit", accountId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }

    protected ResultActions transfer(String key, long from, long to, String amount) throws Exception {
        return mvc.perform(post("/api/v1/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":%s}".formatted(from, to, amount)));
    }

    protected java.math.BigDecimal balance(long accountId) throws Exception {
        return new java.math.BigDecimal(body(mvc.perform(get("/api/v1/accounts/{id}/balance", accountId))
                .andExpect(status().isOk())).get("balance").asText());
    }
}
