package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import nl.ramsolutions.sw.magik.formatting.FormattingProvider;
import nl.ramsolutions.sw.magik.formatting.FormattingWalker;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for formatting errors. */
@Rule(key = FormattingCheck.CHECK_KEY)
public class FormattingCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "Formatting";

  private static final String MESSAGE = "Improper formatting.";
  private static final String MESSAGE_WITH_REASON = "Improper formatting: %s.";
  private static final String FROM_PROPERTIES_INDENT_STRATEGY = "from_properties";
  private static final String DEFAULT_INDENT_STRATEGY = FROM_PROPERTIES_INDENT_STRATEGY;
  private static final String DEFAULT_INDENT_CHARACTER = "tab";
  private static final int DEFAULT_TAB_WIDTH = 8;

  /** The indent strategy used for indentation (null/default). */
  @RuleProperty(
      key = "indent strategy",
      description = "The strategy used for indentation (from_properties/null/relative)",
      defaultValue = "" + DEFAULT_INDENT_STRATEGY,
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String indentStrategy = DEFAULT_INDENT_STRATEGY;

  /** The character used for indentation (tab/space). */
  @RuleProperty(
      key = "indent character",
      description = "The character used for indentation (from_properties/tab/space)",
      defaultValue = "" + DEFAULT_INDENT_CHARACTER,
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String indentCharacter = DEFAULT_INDENT_CHARACTER;

  /** The width of a tab character. */
  @RuleProperty(
      key = "tab width",
      description = "The width of a tab character (-1 for from_properties, default 8)",
      defaultValue = "" + DEFAULT_TAB_WIDTH,
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int tabWidth = DEFAULT_TAB_WIDTH;

  /**
   * Read the properties, overriding with check settings.
   *
   * @return New instance of {@link MagikToolsProperties} with the check settings applied.
   */
  private MagikToolsProperties createMagikToolsProperties() {
    final MagikToolsProperties properties = this.getMagikFile().getProperties();
    final MagikToolsProperties propertiesCopy = new MagikToolsProperties(properties);

    // Override properties with check settings.
    if (!this.indentStrategy.equalsIgnoreCase(FROM_PROPERTIES_INDENT_STRATEGY)) {
      propertiesCopy.setProperty(
          MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_STRATEGY, this.indentStrategy);
    }
    if (!this.indentCharacter.equalsIgnoreCase("from_properties")) {
      propertiesCopy.setProperty(
          MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_CHAR, this.indentCharacter);
    }
    if (this.tabWidth > 0) {
      propertiesCopy.setProperty(
          MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_WIDTH, this.tabWidth);
    }

    return propertiesCopy;
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    final MagikToolsProperties properties = this.createMagikToolsProperties();
    final MagikFormattingSettings formattingSettings = new MagikFormattingSettings(properties);
    final FormattingOptions formattingOptions = formattingSettings.getFormattingOptions();

    // A misconfigured indent strategy is a configuration problem, not a source problem: surface it
    // as an issue at the start of the file instead of formatting with a fallback strategy (which
    // would produce misleading formatting issues).
    if (!formattingSettings.isIndentStrategyValid()) {
      this.addIssue(1, 0, 1, 0, formattingSettings.getIndentStrategyErrorMessage());
      return;
    }

    final Class<? extends FormattingWalker> indentWalker =
        formattingSettings.getIndentStrategyClass();
    final FormattingProvider formattingProvider =
        new FormattingProvider(formattingOptions, indentWalker);
    final AstNode topNode = this.getMagikFile().getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    final List<TextEdit> textEdits = formattingProvider.format(topNodeClone);
    final URI uri = this.getMagikFile().getUri();
    textEdits.forEach(
        textEdit -> {
          final String reason = textEdit.getReason();
          final String message = reason != null ? MESSAGE_WITH_REASON.formatted(reason) : MESSAGE;
          final Range range = textEdit.getRange();
          final Location location = new Location(uri, range);
          this.addIssue(location, message);
        });
  }
}
