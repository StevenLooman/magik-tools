package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import nl.ramsolutions.sw.magik.formatting.FormattingWalker;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for formatting errors. */
@Rule(key = FormattingCheck.CHECK_KEY)
public class FormattingCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "Formatting";

  private static final String MESSAGE = "Improper formatting: %s.";
  private static final String DEFAULT_INDENT_CHARACTER = "tab";
  private static final int DEFAULT_TAB_WIDTH = 8;

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
    final FormattingOptions formattingOptions =
        new FormattingOptions(this.tabWidth, insertSpaces, false, false, false);
    final FormattingWalker walker = new FormattingWalker(formattingOptions);
    final AstNode topNode = this.getMagikFile().getTopNode();
    walker.walkAst(topNode);

    final URI uri = this.getMagikFile().getUri();
    walker
        .getTextEdits()
        .forEach(
            textEdit -> {
              final String reason = textEdit.getReason();
              final String message = String.format(MESSAGE, reason);
              final Range range = textEdit.getRange();
              final Location location = new Location(uri, range);
              this.addIssue(location, message);
            });
  }
}
