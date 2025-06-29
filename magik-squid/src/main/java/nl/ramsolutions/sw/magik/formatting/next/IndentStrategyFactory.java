package nl.ramsolutions.sw.magik.formatting.next;

import java.util.Map;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;

/** Factory for creating indent strategies based on the formatting settings. */
public class IndentStrategyFactory {

  /** Alias for {@link NullIndentWalker}. */
  private static final String STRATEGY_NONE = "none";

  private static final Map<String, Class<? extends FormattingWalker2>> INDENT_STRATEGIES =
      Map.of(
          STRATEGY_NONE,
          NullIndentWalker.class,
          NullIndentWalker.STRATEGY_NAME,
          NullIndentWalker.class,
          RelativeIndentWalker.STRATEGY_NAME,
          RelativeIndentWalker.class);

  private final MagikFormattingSettings formattingSettings;

  public IndentStrategyFactory(final MagikFormattingSettings formattingSettings) {
    this.formattingSettings = formattingSettings;
  }

  /**
   * Creates a list of formatting walkers based on the indent strategy.
   *
   * @return A list of formatting walker classes.
   */
  public Class<? extends FormattingWalker2> create() {
    final String indentStrategy = this.formattingSettings.getIndentStrategy();
    if (!IndentStrategyFactory.INDENT_STRATEGIES.containsKey(indentStrategy)) {
      throw new IllegalArgumentException("Unknown indent strategy: " + indentStrategy);
    }

    final Class<? extends FormattingWalker2> walkerClass =
        IndentStrategyFactory.INDENT_STRATEGIES.get(indentStrategy);
    return walkerClass;
  }
}
