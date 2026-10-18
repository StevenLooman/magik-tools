package nl.ramsolutions.sw.message;

/** Helper for the symbols in a message file, such as the id and the language of a message. */
public final class MessageSymbol {

  private MessageSymbol() {
    // Utility class.
  }

  /**
   * Get the name of a symbol as it is written in a message file, e.g. {@code :Id_1} or {@code :|id
   * with spaces|}.
   *
   * @param symbol Symbol, including the leading colon.
   * @return Name of the symbol.
   */
  public static String nameOf(final String symbol) {
    final StringBuilder builder = new StringBuilder();
    boolean piped = false;
    for (int i = 1; i < symbol.length(); ++i) {
      final char chr = symbol.charAt(i);
      if (chr == '|') {
        piped = !piped;
      } else if (chr == '\\' && !piped && i + 1 < symbol.length()) {
        ++i;
        builder.append(symbol.charAt(i));
      } else {
        builder.append(piped ? chr : Character.toLowerCase(chr));
      }
    }
    return builder.toString();
  }
}
