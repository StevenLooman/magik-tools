package nl.ramsolutions.sw.magik.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

class MagikNumberParserTest {

  @ParameterizedTest
  @CsvSource({
    "'1', 1",
    "'16r10', 16",
    "'16r1ffffffff', 8589934591",
    "'16R1ffffffff', 8589934591",
    "'16e5', 1600000",
    "'16E5', 1600000",
    "'16e+5', 1600000",
    "'16E+5', 1600000",
    "'16&5', 1600000",
    "'16&+5', 1600000",
  })
  void testValidIntegral(final ArgumentsAccessor argumentsAccessor) {
    final String numberStr = argumentsAccessor.getString(0);
    final Long expected = argumentsAccessor.getLong(1);
    final Number number = MagikNumberParser.parseMagikNumber(numberStr);
    if (expected < Integer.MAX_VALUE) {
      final Integer expectedInteger = expected.intValue();
      assertThat(number).isEqualTo(expectedInteger);
    } else {
      assertThat(number).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "'1.0', 1.0",
    "'100.0', 100.0",
    "'16e-5', 0.0001600",
  })
  void testValidFloat(final ArgumentsAccessor argumentsAccessor) {
    final String numberStr = argumentsAccessor.getString(0);
    final Number expected = argumentsAccessor.getDouble(1);
    final Number number = MagikNumberParser.parseMagikNumber(numberStr);
    // Due to floating point precision across platforms.
    assertThat((Double) number).isCloseTo((Double) expected, within(0.0000001));
  }
}
