package io.github.ilya34.bank.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class CardNumberGeneratorTest {

    private final CardNumberGenerator generator = new CardNumberGenerator();

    @RepeatedTest(20)
    void generatedNumberIsSixteenDigitsAndPassesLuhn() {
        String number = generator.generate();

        assertThat(number).matches("\\d{16}").startsWith("400000");
        assertThat(CardNumberGenerator.luhnCheckDigit(number.substring(0, 15)))
                .isEqualTo(number.charAt(15) - '0');
    }

    @Test
    void maskKeepsOnlyLastFourDigits() {
        assertThat(generator.mask("4000001234567899")).isEqualTo("**** **** **** 7899");
    }
}
