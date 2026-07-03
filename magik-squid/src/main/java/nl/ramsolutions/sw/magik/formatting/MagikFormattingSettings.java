package nl.ramsolutions.sw.magik.formatting;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Settings for magik formatting. */
public class MagikFormattingSettings {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikFormattingSettings.class);

  public static final String KEY_MAGIK_FORMATTING_INDENT_STRATEGY =
      "magik.formatting.indentStrategy";
  public static final String KEY_MAGIK_FORMATTING_INDENT_CHAR = "magik.formatting.indentChar";
  public static final String KEY_MAGIK_FORMATTING_INDENT_WIDTH = "magik.formatting.indentWidth";
  public static final String KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE =
      "magik.formatting.insertFinalNewline";
  public static final String KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE =
      "magik.formatting.trimTrailingWhitespace";
  public static final String KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES =
      "magik.formatting.trimFinalNewlines";
  public static final char SPACE = ' ';

  /** Alias for {@link NullIndentWalker}. */
  private static final String STRATEGY_NONE = "none";

  /** Alias for {@link NullIndentWalker}. */
  private static final String STRATEGY_BLANK = "";

  /** Backward compatibility alias for {@link BlockIndentWalker}. */
  private static final String STRATEGY_TAB = "tab";

  /** Backward compatibility alias for {@link VisualIndentWalker}. */
  private static final String STRATEGY_ALIGNMENT = "alignment";

  /** Backward compatibility alias for {@link VisualIndentWalker}. */
  private static final String STRATEGY_RELATIVE = "relative";

  private static final Map<String, Class<? extends FormattingWalker>> INDENT_STRATEGIES =
      Map.of(
          MagikFormattingSettings.STRATEGY_BLANK,
          NullIndentWalker.class,
          MagikFormattingSettings.STRATEGY_NONE,
          NullIndentWalker.class,
          NullIndentWalker.STRATEGY_NAME,
          NullIndentWalker.class,
          MagikFormattingSettings.STRATEGY_TAB,
          BlockIndentWalker.class,
          MagikFormattingSettings.STRATEGY_ALIGNMENT,
          VisualIndentWalker.class,
          BlockIndentWalker.STRATEGY_NAME,
          BlockIndentWalker.class,
          VisualIndentWalker.STRATEGY_NAME,
          VisualIndentWalker.class,
          MagikFormattingSettings.STRATEGY_RELATIVE,
          VisualIndentWalker.class);

  /** Walker used when the configured indent strategy is unknown. */
  private static final Class<? extends FormattingWalker> DEFAULT_INDENT_WALKER =
      NullIndentWalker.class;

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikFormattingSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get the indent strategy.
   *
   * @return The indent strategy, defaults to "default".
   */
  public String getIndentStrategy() {
    return this.properties.getPropertyString(KEY_MAGIK_FORMATTING_INDENT_STRATEGY, "null");
  }

  /**
   * Check whether the configured indent strategy is known.
   *
   * @return {@code true} if the configured indent strategy resolves to a formatting walker.
   */
  public boolean isIndentStrategyValid() {
    return MagikFormattingSettings.INDENT_STRATEGIES.containsKey(this.getIndentStrategy());
  }

  /**
   * Build a human-readable message describing why the configured indent strategy is unknown,
   * including a suggestion for the closest known value and the list of allowed values.
   *
   * @return The message describing the misconfiguration.
   */
  public String getIndentStrategyErrorMessage() {
    final String indentStrategy = this.getIndentStrategy();
    final List<String> canonicalStrategies = MagikFormattingSettings.getCanonicalStrategyNames();
    final String allowedStrategies =
        canonicalStrategies.stream()
            .map(strategy -> "\"" + strategy + "\"")
            .collect(Collectors.joining(", "));

    final String suggestion =
        MagikFormattingSettings.findClosestStrategy(indentStrategy, canonicalStrategies);
    final String didYouMean = suggestion != null ? " Did you mean \"" + suggestion + "\"?" : "";

    return "Unknown indent strategy: \""
        + indentStrategy
        + "\" for setting "
        + MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_STRATEGY
        + "."
        + didYouMean
        + " Allowed values are: "
        + allowedStrategies
        + ".";
  }

  /**
   * Get the formatting walker for the configured indent strategy. When the configured strategy is
   * unknown, a warning is logged and the default walker is returned so formatting degrades
   * gracefully instead of failing.
   *
   * @return The formatting walker class.
   */
  public Class<? extends FormattingWalker> getIndentStrategyClass() {
    final String indentStrategy = this.getIndentStrategy();
    final Class<? extends FormattingWalker> walkerClass =
        MagikFormattingSettings.INDENT_STRATEGIES.get(indentStrategy);
    if (walkerClass == null) {
      LOGGER.warn(this.getIndentStrategyErrorMessage());
      return MagikFormattingSettings.DEFAULT_INDENT_WALKER;
    }

    return walkerClass;
  }

  /**
   * Get the sorted, distinct canonical strategy names, as declared by each formatting walker's
   * {@code STRATEGY_NAME} constant.
   *
   * @return The canonical strategy names.
   */
  private static List<String> getCanonicalStrategyNames() {
    return MagikFormattingSettings.INDENT_STRATEGIES.values().stream()
        .distinct()
        .map(MagikFormattingSettings::getStrategyName)
        .sorted()
        .toList();
  }

  /**
   * Get the canonical strategy name for a {@link FormattingWalker} class, as declared by its {@code
   * STRATEGY_NAME} constant.
   *
   * @param walkerClass The formatting walker class.
   * @return The canonical strategy name.
   */
  private static String getStrategyName(final Class<? extends FormattingWalker> walkerClass) {
    try {
      return (String) walkerClass.getField("STRATEGY_NAME").get(null);
    } catch (final NoSuchFieldException | IllegalAccessException exception) {
      throw new IllegalStateException(
          "Formatting walker "
              + walkerClass.getName()
              + " does not declare a public static STRATEGY_NAME field",
          exception);
    }
  }

  /**
   * Find the known strategy closest to the given (unknown) value, if one is close enough to be a
   * likely typo.
   *
   * @param value The unknown strategy value.
   * @param candidates The known strategy names.
   * @return The closest candidate, or {@code null} if none is close enough.
   */
  private static String findClosestStrategy(final String value, final List<String> candidates) {
    String closest = null;
    int closestDistance = Integer.MAX_VALUE;
    for (final String candidate : candidates) {
      final int distance = StringUtils.levenshteinDistance(value, candidate);
      if (distance < closestDistance) {
        closestDistance = distance;
        closest = candidate;
      }
    }

    // Only suggest when the value is a plausible typo of the candidate.
    final int threshold = Math.max(2, value.length() / 2);
    return closestDistance <= threshold ? closest : null;
  }

  /**
   * Get the indent character. Defaults to tab.
   *
   * @return The indent character, either a tab or a space.
   */
  public char getIndentChar() {
    return this.properties.getPropertyString(KEY_MAGIK_FORMATTING_INDENT_CHAR, "tab").equals("tab")
        ? '\t'
        : ' ';
  }

  /**
   * Get the indent width.
   *
   * @return The indent width, defaults to 8.
   */
  public int getIndentWidth() {
    return this.properties.getPropertyInteger(KEY_MAGIK_FORMATTING_INDENT_WIDTH, 8);
  }

  /**
   * Check if the formatter should insert a final newline.
   *
   * @return True if a final newline should be inserted, defaults to true.
   */
  public boolean insertFinalNewline() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE, true);
  }

  /**
   * Check if the formatter should trim trailing whitespace.
   *
   * @return True if trailing whitespace should be trimmed, defaults to true.
   */
  public boolean trimTrailingWhitespace() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE, true);
  }

  /**
   * Check if the formatter should trim final newlines.
   *
   * @return True if final newlines should be trimmed, defaults to true.
   */
  public boolean trimFinalNewlines() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES, true);
  }

  /**
   * Get the indent string based on the current settings.
   *
   * @return The indent string, either a tab or spaces based on the indent character and width.
   */
  public String getIndent() {
    final char indentChar = this.getIndentChar();
    if (indentChar == '\t') {
      return String.valueOf(indentChar);
    }

    final int indentWidth = this.getIndentWidth();
    return String.valueOf(indentChar).repeat(indentWidth);
  }

  /**
   * Get the {@link FormattingOptions} based on the current settings.
   *
   * @return A new instance of {@link FormattingOptions} based on the current settings.
   */
  public FormattingOptions getFormattingOptions() {
    return new FormattingOptions(
        this.getIndentWidth(),
        this.getIndentChar() == SPACE,
        this.insertFinalNewline(),
        this.trimTrailingWhitespace(),
        this.trimFinalNewlines());
  }
}
