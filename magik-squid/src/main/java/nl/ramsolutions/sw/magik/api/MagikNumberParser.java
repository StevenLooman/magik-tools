package nl.ramsolutions.sw.magik.api;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Scanner;

/** Utility class to parse numbers. */
public final class MagikNumberParser {

  /**
   * Parse a Magik number.
   *
   * <p>Throws an exception when number could not be parsed.
   *
   * @param numberStr
   * @return Parsed number.
   */
  public static Number parseMagikNumber(final String numberStr) {
    final Number number;
    if (numberStr.contains("r") || numberStr.contains("R")) {
      try (final Scanner scanner = new Scanner(numberStr)) {
        scanner.useDelimiter("r|R");
        final int base = scanner.nextInt();
        number = scanner.nextLong(base);
      }
    } else if (numberStr.contains("e") || numberStr.contains("E") || numberStr.contains("&")) {
      try (final Scanner scanner = new Scanner(numberStr)) {
        scanner.useDelimiter("e|E|&");
        final String exponentStr = scanner.next();
        final Number exponent = MagikNumberParser.parseMagikNumber(exponentStr);
        final int n = scanner.nextInt();
        final Number pow;
        if (n < 0) {
          pow = Math.pow(10, n);
        } else {
          pow = Double.valueOf(Math.pow(10, n)).longValue();
        }
        if (exponent instanceof final Double exponentDouble) {
          number = exponentDouble * (double) pow;
        } else if (exponent instanceof final Long exponentLong) {
          if (pow instanceof final Double powDouble) {
            number = exponentLong * powDouble;
          } else {
            number = exponentLong * (Long) pow;
          }
        } else if (exponent instanceof final Integer exponentInt) {
          if (pow instanceof final Double powDouble) {
            number = exponentInt * powDouble;
          } else {
            number = exponentInt * (Long) pow;
          }
        } else {
          throw new NumberFormatException();
        }
      }
    } else if (numberStr.contains(".")) {
      number = Double.parseDouble(numberStr);
    } else {
      number = Long.parseLong(numberStr);
    }

    if (number instanceof final Long numberLong) {
      if (numberLong < Integer.MAX_VALUE) {
        return number.intValue();
      }
    }

    return number;
  }

  @CheckForNull
  public static Number parseMagikNumberSafe(final String numberStr) {
    try {
      return MagikNumberParser.parseMagikNumber(numberStr);
    } catch (final NumberFormatException exception) {
      // Pass.
    }

    return null;
  }
}
