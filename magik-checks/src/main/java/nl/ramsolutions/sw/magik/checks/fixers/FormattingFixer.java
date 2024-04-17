package nl.ramsolutions.sw.magik.checks.fixers;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheckFixer;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import nl.ramsolutions.sw.magik.formatting.FormattingWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormattingFixer extends MagikCheckFixer {

  private static final Logger LOGGER = LoggerFactory.getLogger(FormattingFixer.class);

  @Override
  public List<CodeAction> provideCodeActions(final MagikFile magikFile, final Range range) {
    final AstNode node = magikFile.getTopNode();
    if (!this.canFormat(magikFile)) {
      LOGGER.warn("Cannot format due to syntax errors");
      return Collections.emptyList();
    }

    final FormattingOptions formattingOptions = new FormattingOptions(8, false, true, true, false);
    FormattingWalker walker;
    try {
      walker = new FormattingWalker(formattingOptions);
    } catch (final IOException exception) {
      LOGGER.error("Error creating formatter", exception);
      return Collections.emptyList();
    }

    walker.walkAst(node);
    return walker.getTextEdits().stream()
        .filter(edit -> edit.getRange().overlapsWith(range))
        .map(textEdit -> new CodeAction("Formatting", textEdit))
        .toList();
  }

  private boolean canFormat(final MagikFile magikFile) {
    final AstNode node = magikFile.getTopNode();
    return node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR) == null;
  }
}
