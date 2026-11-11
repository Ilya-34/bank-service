package io.github.ilya34.bank.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates demo account numbers like BS12345678901234 (not IBAN, not real). */
@Component
public class AccountNumberGenerator {

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder("BS");
        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
