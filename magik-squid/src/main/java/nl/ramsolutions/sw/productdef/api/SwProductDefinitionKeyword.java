package nl.ramsolutions.sw.productdef.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/** Keywords for {@link SwProductDefinitionGrammar}. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum SwProductDefinitionKeyword implements GrammarRuleKey {
  DESCRIPTION,
  DO_NOT_TRANSLATE,
  REQUIRES,
  OPTIONAL,
  TITLE,
  VERSION,
  LAYERED_PRODUCT,
  CUSTOMISATION_PRODUCT,
  CONFIG_PRODUCT,
  END;

  /**
   * Get all keyword values.
   *
   * @return Keyword values
   */
  public static String[] keywordValues() {
    final String[] keywordsValue = new String[SwProductDefinitionKeyword.values().length];
    int idx = 0;
    for (final SwProductDefinitionKeyword keyword : SwProductDefinitionKeyword.values()) {
      keywordsValue[idx] = keyword.getValue();
      idx++;
    }
    return keywordsValue;
  }

  /**
   * Get value of keyword.
   *
   * @return Value of keyword
   */
  public String getValue() {
    return this.toString().toLowerCase();
  }
}
