package nl.ramsolutions.sw.magik.api;

import java.util.Locale;
import org.sonar.sslr.grammar.GrammarRuleKey;

/** Magik keywords. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum MagikKeyword implements GrammarRuleKey {
  BLOCK,
  ENDBLOCK,
  RETURN,
  METHOD,
  ENDMETHOD,
  ITER,
  PRIVATE,
  ABSTRACT,
  GATHER,
  SCATTER,
  ALLRESULTS,
  OPTIONAL,
  PACKAGE,
  PRAGMA,
  HANDLING,
  WITH,
  DEFAULT,
  OR,
  ORIF,
  AND,
  ANDIF,
  XOR,
  IS,
  ISNT,
  CF,
  NOT,
  DIV,
  MOD,
  THROW,
  PROTECT,
  LOCKING,
  PROTECTION,
  ENDPROTECT,
  TRY,
  WHEN,
  ENDTRY,
  CATCH,
  ENDCATCH,
  LOCK,
  ENDLOCK,
  IF,
  THEN,
  ELIF,
  ELSE,
  ENDIF,
  FOR,
  WHILE,
  OVER,
  LOOP,
  FINALLY,
  ENDLOOP,
  LOOPBODY,
  LEAVE,
  CONTINUE,
  PROC,
  ENDPROC,
  LOCAL,
  CONSTANT,
  RECURSIVE,
  GLOBAL,
  DYNAMIC,
  IMPORT,
  TRUE,
  FALSE,
  MAYBE,
  UNSET,
  SELF,
  CLONE,
  SUPER,
  PRIMITIVE,
  THISTHREAD,
  CLASS;

  /**
   * Get all keyword values.
   *
   * @return Keyword values
   */
  public static String[] keywordValues() {
    final String[] keywordsValue = new String[MagikKeyword.values().length];
    int idx = 0;
    for (final MagikKeyword keyword : MagikKeyword.values()) {
      keywordsValue[idx] = keyword.getValue();
      idx++;
    }
    return keywordsValue;
  }

  /**
   * Get value of keyword, prefixed with <code>_</code>.
   *
   * @return Value of keyword
   */
  public String getValue() {
    return "_" + this.toString().toLowerCase(Locale.ENGLISH);
  }
}
