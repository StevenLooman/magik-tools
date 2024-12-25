package nl.ramsolutions.sw.magik.languageserver.formatting;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.formatting.FormattingWalker;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextEdit;

/** Formatting provider. */
public class FormattingProvider {

  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDocumentFormattingProvider(true);
    capabilities.setDocumentRangeFormattingProvider(true);
  }

  /**
   * Provide formatting for text.
   *
   * @param magikFile Magik file.
   * @param options Formatting options
   * @return {@link TextEdit}s.
   * @throws IOException -
   */
  public List<TextEdit> provideFormatting(
      final MagikFile magikFile, final FormattingOptions options) {
    final AstNode node = magikFile.getTopNode();

    final nl.ramsolutions.sw.magik.formatting.FormattingOptions magikToolsFormattingOptions =
        Lsp4jConversion.formattingOptionsFromLsp4j(options);
    final FormattingWalker walker = new FormattingWalker(magikToolsFormattingOptions);
    walker.walkAst(node);
    final List<nl.ramsolutions.sw.magik.TextEdit> textEdits = walker.getTextEdits();
    return textEdits.stream().map(Lsp4jConversion::textEditToLsp4j).toList();
  }

  /**
   * Provide formatting for text in range.
   *
   * @param magikFile Magik file.
   * @param options Formatting options.
   * @param range Range.
   * @return {@link TextEdit}s.
   * @throws IOException -
   */
  public List<TextEdit> provideRangeFormatting(
      final MagikFile magikFile, final FormattingOptions options, final Range range) {
    final List<org.eclipse.lsp4j.TextEdit> textEdits = this.provideFormatting(magikFile, options);
    return textEdits.stream().filter(edit -> this.isEditInRange(edit, range)).toList();
  }

  /**
   * Test if a given text edit's range overlaps with the specified range.
   *
   * @param edit the text edit to check
   * @param range the range to check against
   * @return true if the text edit's range overlaps with the specified range, false otherwise
   */
  private boolean isEditInRange(TextEdit edit, Range range) {
    final org.eclipse.lsp4j.Range editRange = edit.getRange();
    final Range editMagikRange = Lsp4jConversion.rangeFromLsp4j(editRange);

    return range.overlapsWith(editMagikRange);
  }

  /**
   * Test if formatting can be provided.
   *
   * <p>A SYNTAX_ERROR prevents formatting.
   *
   * @param magikFile Magik file.
   * @return False if AST contains a SYNTAX_ERROR, true otherwise.
   */
  public boolean canFormat(final MagikFile magikFile) {
    final AstNode node = magikFile.getTopNode();
    return node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR) == null;
  }
}
