package nl.ramsolutions.sw.magik.api;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class to parse numbers. */
public final class MagikNumberParser {

  private static final String SIGN_REGEXP = "[+-]?";
  private static final String EXPONENT_REGEXP = "(?:[eE&](" + SIGN_REGEXP + "\\d+))?";
  private static final Pattern RADIX_PATTERN =
      Pattern.compile("(" + SIGN_REGEXP + ")(\\d+)[rR]([0-9a-zA-Z]+)");
  private static final Pattern INTEGER_PATTERN =
      Pattern.compile("(" + SIGN_REGEXP + "\\d+)" + EXPONENT_REGEXP);
  private static final Pattern FLOAT_PATTERN =
      Pattern.compile("(" + SIGN_REGEXP + "\\d+\\.\\d+)" + EXPONENT_REGEXP);
  // Bounds the cost of expanding an integer with an exponent.
  private static final int MAX_INTEGER_EXPONENT = 1000;
  private static final int INTEGER_BITS = 31;
  private static final int LONG_BITS = 63;

  private MagikNumberParser() {}

  /**
   * Parse a Magik number.
   *
   * <p>An integral number is returned as the narrowest of {@link Integer}, {@link Long} and {@link
   * BigInteger} that holds it, any other number as a {@link Double}.
   *
   * @param numberStr Number literal.
   * @return Parsed number.
   * @throws NumberFormatException If the number could not be parsed.
   */
  public static Number parseMagikNumber(final String numberStr) {
    final Matcher radixMatcher = RADIX_PATTERN.matcher(numberStr);
    if (radixMatcher.matches()) {
      return MagikNumberParser.parseRadixNumber(radixMatcher);
    }

    final Matcher integerMatcher = INTEGER_PATTERN.matcher(numberStr);
    if (integerMatcher.matches()) {
      return MagikNumberParser.parseIntegerNumber(integerMatcher);
    }

    final Matcher floatMatcher = FLOAT_PATTERN.matcher(numberStr);
    if (floatMatcher.matches()) {
      return MagikNumberParser.parseFloatNumber(floatMatcher);
    }

    throw new NumberFormatException("For input string: \"" + numberStr + "\"");
  }

  /**
   * Parse a Magik number.
   *
   * @param numberStr Number literal.
   * @return Parsed number, or null if the number could not be parsed.
   */
  @CheckForNull
  public static Number parseMagikNumberSafe(final String numberStr) {
    try {
      return MagikNumberParser.parseMagikNumber(numberStr);
    } catch (final NumberFormatException exception) {
      // Pass.
    }

    return null;
  }

  private static Number parseRadixNumber(final Matcher matcher) {
    final String signStr = matcher.group(1);
    final String radixStr = matcher.group(2);
    final String digitsStr = matcher.group(3);
    final int radix = Integer.parseInt(radixStr);
    final BigInteger value = new BigInteger(signStr + digitsStr, radix);
    return MagikNumberParser.narrowInteger(value);
  }

  private static Number parseIntegerNumber(final Matcher matcher) {
    final String mantissaStr = matcher.group(1);
    final String exponentStr = matcher.group(2);
    final BigInteger mantissa = new BigInteger(mantissaStr);
    if (exponentStr == null) {
      return MagikNumberParser.narrowInteger(mantissa);
    }

    if (exponentStr.startsWith("-")) {
      return Double.valueOf(mantissaStr + "e" + exponentStr);
    }

    final int exponent = Integer.parseInt(exponentStr);
    if (exponent > MAX_INTEGER_EXPONENT) {
      throw new NumberFormatException("Exponent too large: " + exponentStr);
    }

    final BigInteger factor = BigInteger.TEN.pow(exponent);
    final BigInteger value = mantissa.multiply(factor);
    return MagikNumberParser.narrowInteger(value);
  }

  private static Number parseFloatNumber(final Matcher matcher) {
    final String mantissaStr = matcher.group(1);
    final String exponentStr = matcher.group(2);
    if (exponentStr == null) {
      return Double.valueOf(mantissaStr);
    }

    return Double.valueOf(mantissaStr + "e" + exponentStr);
  }

  private static Number narrowInteger(final BigInteger value) {
    final int bitLength = value.bitLength();
    if (bitLength <= INTEGER_BITS) {
      return value.intValue();
    } else if (bitLength <= LONG_BITS) {
      return value.longValue();
    }

    return value;
  }
}
