package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import nl.ramsolutions.sw.magik.formatting.next.FormattingProvider;
import nl.ramsolutions.sw.magik.formatting.next.FormattingWalker2;
import nl.ramsolutions.sw.magik.formatting.next.IndentStrategyFactory;
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
      description = "The character used for indentation (tab/space)",
      defaultValue = "" + DEFAULT_INDENT_CHARACTER,
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String indentCharacter = DEFAULT_INDENT_CHARACTER;

  /** The width of a tab character. */
  @RuleProperty(
      key = "tab width",
      description = "The width of a tab character",
      defaultValue = "" + DEFAULT_TAB_WIDTH,
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int tabWidth = DEFAULT_TAB_WIDTH;

  @Override
  protected void walkPostMagik(final AstNode node) {
    final boolean insertSpaces = this.indentCharacter.equalsIgnoreCase("space");
    // TODO: Shouldn't these use the same options as the formatter? I.e.,
    // create this from MagikFormattingSettings?
    final FormattingOptions formattingOptions =
        new FormattingOptions(this.tabWidth, insertSpaces, false, false, false);

    // TODO: Use the settings or the check settings?
    final MagikToolsProperties properties = this.getMagikFile().getProperties();
    final MagikToolsProperties propertiesCopy = new MagikToolsProperties(properties);
    if (!this.indentStrategy.equalsIgnoreCase(FROM_PROPERTIES_INDENT_STRATEGY)) {
      propertiesCopy.setProperty(
          MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_STRATEGY, this.indentStrategy);
    }
    final MagikFormattingSettings formattingSettings = new MagikFormattingSettings(propertiesCopy);
    final IndentStrategyFactory indentStrategyFactory =
        new IndentStrategyFactory(formattingSettings);
    final Class<? extends FormattingWalker2> indentWalker = indentStrategyFactory.create();
    final FormattingProvider formattingProvider =
        new FormattingProvider(formattingOptions, indentWalker);
    final AstNode topNode = this.getMagikFile().getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    final List<TextEdit> textEdits = formattingProvider.format(topNodeClone);
    final URI uri = this.getMagikFile().getUri();
    textEdits.forEach(
        textEdit -> {
          final String reason = textEdit.getReason();
          final String message =
              reason != null ? String.format(MESSAGE_WITH_REASON, reason) : MESSAGE;
          final Range range = textEdit.getRange();
          final Location location = new Location(uri, range);
          this.addIssue(location, message);
        });
  }
}
