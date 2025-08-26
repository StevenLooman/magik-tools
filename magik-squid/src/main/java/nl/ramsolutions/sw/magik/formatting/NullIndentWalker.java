package nl.ramsolutions.sw.magik.formatting;

import nl.ramsolutions.sw.TokenTriviaEditor;

/** Null indent walker. */
public class NullIndentWalker extends FormattingWalker {

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
