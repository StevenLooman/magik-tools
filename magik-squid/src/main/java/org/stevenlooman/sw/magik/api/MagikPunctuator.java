package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

public enum MagikPunctuator implements GrammarRuleKey {
  AT("@"),
  PAREN_L("("), PAREN_R(")"),
  BRACE_L("{"), BRACE_R("}"),
  SQUARE_L("["), SQUARE_R("]"),
  NOT("~"),
  DOT("."), COMMA(","), SEMICOLON(";"), COLON(":"),
  PLUS("+"), MINUS("-"), STAR("*"), DIV("/"),
  EXP("**"),
  EQ("="), NEQ("~="), NE("<>"), LT("<"), LE("<="), GT(">"), GE(">="),
  CHEVRON("<<"), BOOT_CHEVRON("^<<"),
  EMIT(">>"),
  DOLLAR("$"),
  ;

  private final String value;

  MagikPunctuator(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}