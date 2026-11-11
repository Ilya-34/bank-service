package io.github.ilya34.bank.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CustomerApiTest extends ApiTestBase {

    private static final String VALID = """
            {"firstName":"Anna","lastName":"Ivanova","email":"%s","phone":"+375291234567"}""";

    @Test
    void createAndGetCustomer() throws Exception {
        String email = "anna-" + System.nanoTime() + "@example.com";
        long id = body(mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/customers/")))
                .andExpect(jsonPath("$.email").value(email))).get("id").asLong();

        mvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void invalidPayloadReturns400ProblemDetailWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"X\",\"email\":\"not-an-email\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='phone')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')]").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateEmailReturns409CaseInsensitively() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.formatted(email))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.formatted(email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void updateChangesFields() throws Exception {
        long id = createCustomer();

        mvc.perform(put("/api/v1/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Maria\",\"lastName\":\"Petrova\","
                                + "\"email\":\"maria" + id + "@example.com\",\"phone\":\"+48123456789\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.phone").value("+48123456789"));
    }

    @Test
    void listIsPaginated() throws Exception {
        for (int i = 0; i < 3; i++) {
            createCustomer();
        }

        mvc.perform(get("/api/v1/customers").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }
}
