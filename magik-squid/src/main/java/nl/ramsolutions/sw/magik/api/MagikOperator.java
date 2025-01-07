package nl.ramsolutions.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/** Magik operators. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum MagikOperator implements GrammarRuleKey {
  NOT("~"),
  PLUS("+"),
  MINUS("-"),
  STAR("*"),
  DIV("/"),
  EXP("**"),
  EQ("="),
  NEQ("~="),
  NE("<>"),
  LT("<"),
  LE("<="),
  GT(">"),
  GE(">="),
  CHEVRON("<<"),
  BOOT_CHEVRON("^<<");

  private final String value;

  /**
   * Get all operator values.
   *
   * @return Operator values
   */
  public static String[] operatorValues() {
    final String[] operatorValues = new String[MagikOperator.values().length];
    int idx = 0;
    for (final MagikOperator punctuator : MagikOperator.values()) {
      operatorValues[idx] = punctuator.getValue();
      idx++;
    }
    return operatorValues;
  }

  MagikOperator(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
