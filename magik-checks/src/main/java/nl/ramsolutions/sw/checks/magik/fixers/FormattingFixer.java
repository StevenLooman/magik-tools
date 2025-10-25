package nl.ramsolutions.sw.checks.magik.fixers;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.MagikCodeActionSupplier;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import nl.ramsolutions.sw.magik.formatting.FormattingProvider;
import nl.ramsolutions.sw.magik.formatting.FormattingWalker;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fixer for formatting issues. */
public class FormattingFixer extends MagikCodeActionSupplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(FormattingFixer.class);

  @Override
  public List<CodeAction> provideMagikCodeActions(final MagikFile magikFile, final Range range) {
    if (!this.canFormat(magikFile)) {
      LOGGER.warn("Cannot format due to syntax errors");
      return Collections.emptyList();
    }

    final MagikToolsProperties properties = magikFile.getProperties();
    final MagikFormattingSettings formattingSettings = new MagikFormattingSettings(properties);
    final FormattingOptions formattingOptions = formattingSettings.getFormattingOptions();
    final Class<? extends FormattingWalker> indentWalker =
        formattingSettings.getIndentStrategyClass();
    final FormattingProvider formattingProvider =
        new FormattingProvider(formattingOptions, indentWalker);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    final List<TextEdit> textEdits = formattingProvider.format(topNodeClone);
    return textEdits.stream()
        .filter(edit -> edit.getRange().overlapsWith(range))
        .map(textEdit -> new CodeAction("Fix formatting", textEdit))
        .toList();
  }

  private boolean canFormat(final MagikFile magikFile) {
    final AstNode node = magikFile.getTopNode();
    return node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR) == null;
  }
}
