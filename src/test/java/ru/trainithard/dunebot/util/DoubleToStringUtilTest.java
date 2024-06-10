package ru.trainithard.dunebot.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleToStringUtilTest {

    @ParameterizedTest
    @CsvSource(value = {"1.0, 1", "1.00, 1", "1.01, 1.01", "0.0, 0", "0.1, 0.1", "0.01, 0.01", "4.20, 4.2",
            "43.21, 43.21", "43.01, 43.01", "43.10, 43.1", "1, 1", "1.00000, 1", "1.00100, 1.001"})
    void getStrippedZeroesString(double input, String expectedOutput) {
        String actualOutput = DoubleToStringUtil.getStrippedZeroesString(input);

        assertThat(actualOutput).isEqualTo(expectedOutput);
    }
}
