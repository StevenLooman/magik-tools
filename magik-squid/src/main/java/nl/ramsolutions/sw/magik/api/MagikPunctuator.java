package nl.ramsolutions.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/** Magik punctuators. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum MagikPunctuator implements GrammarRuleKey {
  AT("@"),
  PAREN_L("("),
  PAREN_R(")"),
  BRACE_L("{"),
  BRACE_R("}"),
  SQUARE_L("["),
  SQUARE_R("]"),
  DOT("."),
  COMMA(","),
  SEMICOLON(";"),
  COLON(":"),
  EMIT(">>"),
  DOLLAR("$");

  private final String value;

  /**
   * Get all punctuator values.
   *
   * @return Punctuator values
   */
  public static String[] punctuatorValues() {
    final String[] punctuatorValuess = new String[MagikPunctuator.values().length];
    int idx = 0;
    for (final MagikPunctuator punctuator : MagikPunctuator.values()) {
      punctuatorValuess[idx] = punctuator.getValue();
      idx++;
    }
    return punctuatorValuess;
  }

  MagikPunctuator(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
