package io.github.ilya34.bank.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates fake test card numbers (Luhn-valid, fixed 400000 prefix) and masks them.
 * The full number is only ever held in memory; the application stores the masked form.
 */
@Component
public class CardNumberGenerator {

    private static final String TEST_PREFIX = "400000";
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(TEST_PREFIX);
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    public String mask(String number) {
        return "**** **** **** " + number.substring(number.length() - 4);
    }

    static int luhnCheckDigit(String partial) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = partial.length() - 1; i >= 0; i--) {
            int d = partial.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }
}
