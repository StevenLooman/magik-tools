package nl.ramsolutions.sw.moduledef.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/** Keywords for {@link ModuleDefinitionGrammar}. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ModuleDefinitionKeyword implements GrammarRuleKey {
  CONDITION_MESSAGE_ACCESSOR,
  DESCRIPTION,
  DO_NOT_TRANSLATE,
  HIDDEN,
  INSTALL_REQUIRES,
  LANGUAGE,
  MESSAGES,
  OPTIONAL,
  REQUIRED_BY,
  REQUIRES,
  REQUIRES_JAVA,
  REQUIRES_DATAMODEL,
  TEMPLATES,
  TEST,
  TESTS_MODULES,
  ACE_INSTALLATION,
  AUTH_INSTALLATION,
  CASE_INSTALLATION,
  STYLE_INSTALLATION,
  SYSTEM_INSTALLATION,
  END,

  NAME,
  FRAMEWORK,
  TOPICS,
  ARGS,
  LABEL,
  TOPIC,
  ARG;

  /**
   * Get all keyword values.
   *
   * @return Keyword values
   */
  public static String[] keywordValues() {
    final String[] keywordsValue = new String[ModuleDefinitionKeyword.values().length];
    int idx = 0;
    for (final ModuleDefinitionKeyword keyword : ModuleDefinitionKeyword.values()) {
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
