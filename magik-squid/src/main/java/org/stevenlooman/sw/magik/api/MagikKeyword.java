package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum MagikKeyword implements GrammarRuleKey {
  BLOCK, ENDBLOCK,
  RETURN,
  METHOD, ENDMETHOD,
  ITER, PRIVATE, ABSTRACT,
  GATHER,
  SCATTER,
  ALLRESULTS,
  OPTIONAL,
  PACKAGE,
  PRAGMA,
  HANDLING, WITH, DEFAULT,
  OR, ORIF,
  AND, ANDIF,
  XOR,
  IS, ISNT,
  CF,
  NOT,
  DIV, MOD,
  THROW,
  PROTECT, PROTECTION, ENDPROTECT,
  TRY, WHEN, ENDTRY,
  CATCH, ENDCATCH,
  LOCK, ENDLOCK,
  IF, THEN, ELIF, ELSE, ENDIF,
  FOR, OVER, LOOP, FINALLY, ENDLOOP,
  LOOPBODY,
  LEAVE, CONTINUE,
  PROC, ENDPROC,
  LOCAL, CONSTANT, RECURSIVE, GLOBAL, DYNAMIC, IMPORT,
  TRUE, FALSE, MAYBE,
  UNSET,
  SELF, CLONE,
  SUPER,
  PRIMITIVE,
  THISTHREAD,
  ;

  /**
   * Get all keyword values.
   * @return Keyword values
   */
  public static String[] keywordValues() {
    String[] keywordsValue = new String[MagikKeyword.values().length];
    int idx = 0;
    for (MagikKeyword keyword : MagikKeyword.values()) {
      keywordsValue[idx] = keyword.getValue();
      idx++;
    }
    return keywordsValue;
  }

  /**
   * Get all keywords.
   * @return Keywords
   */
  public static List<MagikKeyword> keywords() {
    return Arrays.stream(values()).collect(Collectors.toList());
  }

  /**
   * Get value of keyword, prefixed with <code>_</code>.
   * @return Value of keyword
   */
  public String getValue() {
    return "_" + toString().toLowerCase(Locale.ENGLISH);
  }
}
