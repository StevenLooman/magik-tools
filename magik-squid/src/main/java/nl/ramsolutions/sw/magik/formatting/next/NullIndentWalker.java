package nl.ramsolutions.sw.magik.formatting.next;

import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

/** Null indent walker. */
public class NullIndentWalker extends FormattingWalker2 {

  public static final String STRATEGY_NAME = "null";

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @param tokenEditor Token trivia editor.
   */
  NullIndentWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }
}
