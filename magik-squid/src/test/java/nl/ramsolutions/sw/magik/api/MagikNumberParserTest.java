package nl.ramsolutions.sw.magik.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    "'2147483647', 2147483647",
    "'2147483648', 2147483648",
    "'-5000000000', -5000000000",
  })
  void testValidIntegral(final ArgumentsAccessor argumentsAccessor) {
    final String numberStr = argumentsAccessor.getString(0);
    final Long expected = argumentsAccessor.getLong(1);
    final Number number = MagikNumberParser.parseMagikNumber(numberStr);
    if (expected >= Integer.MIN_VALUE && expected <= Integer.MAX_VALUE) {
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
    "'1.5e6', 1500000.0",
    "'2.0e3', 2000.0",
    "'1.5E6', 1500000.0",
    "'1.5&6', 1500000.0",
    "'1.5e30', 1.5e30",
  })
  void testValidFloat(final ArgumentsAccessor argumentsAccessor) {
    final String numberStr = argumentsAccessor.getString(0);
    final Number expected = argumentsAccessor.getDouble(1);
    final Number number = MagikNumberParser.parseMagikNumber(numberStr);
    // Due to floating point precision across platforms.
    assertThat((Double) number).isCloseTo((Double) expected, within(0.0000001));
  }

  @ParameterizedTest
  @CsvSource({
    "'99999999999999999999', 99999999999999999999",
    "'2e19', 20000000000000000000",
    "'10&18', 10000000000000000000",
    "'16rfffffffffffffffff', 295147905179352825855",
  })
  void testValidBeyondLong(final String numberStr, final String expectedStr) {
    final BigInteger expected = new BigInteger(expectedStr);
    final Number number = MagikNumberParser.parseMagikNumber(numberStr);
    assertThat(number).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2r101e3",
        "16r1.8",
        "16r10&2",
        "1.5r3",
        "1e1.5",
        "0r0",
        "1r0",
        "37r10",
        "1e999999999",
      })
  void testUnparsableSafe(final String numberStr) {
    final Number number = MagikNumberParser.parseMagikNumberSafe(numberStr);
    assertThat(number).isNull();
  }
}
