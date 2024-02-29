package nl.ramsolutions.sw.magik.languageserver.selectionrange;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.ServerCapabilities;

/** Selection range provider. */
public class SelectionRangeProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setSelectionRangeProvider(true);
  }

  /**
   * Provide selection ranges.
   *
   * @param magikFile Magik file.
   * @param positions Positions in magik source.
   * @return Selection ranges.
   */
  public List<SelectionRange> provideSelectionRanges(
      final MagikTypedFile magikFile, final List<Position> positions) {
    final var topNode = magikFile.getTopNode();
    return topNode.getTokens().stream()
        .filter(token -> token.getType() != GenericTokenType.EOF)
        .map(this::createSelectionRange)
        .toList();
  }

  private SelectionRange createSelectionRange(final Token token) {
    final var range = new Range(token);
    final var lsp4jRange = Lsp4jConversion.rangeToLsp4j(range);
    return new SelectionRange(lsp4jRange, null);
  }
}
