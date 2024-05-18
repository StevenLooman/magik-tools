package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Code action provider. */
public class CodeActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CodeActionProvider.class);

  private final MagikChecksCodeActionProvider checksCodeActionProvider;
  private final MagikTypedChecksCodeActionProvider typedChecksCodeActionProvider;

  public CodeActionProvider(final MagikToolsProperties properties) {
    this.checksCodeActionProvider = new MagikChecksCodeActionProvider(properties);
    this.typedChecksCodeActionProvider = new MagikTypedChecksCodeActionProvider(properties);
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setCodeActionProvider(true);
  }

  /**
   * Provide code actions.
   *
   * @param magikFile Magik file.
   * @param range Range to provide code actions for.
   * @param context Code action context.
   * @return List of code actions.
   * @throws IOException -
   */
  public List<CodeAction> provideCodeActions(
      final MagikTypedFile magikFile, final Range range, final CodeActionContext context) {
    try {
      return Stream.concat(
              this.checksCodeActionProvider.provideCodeActions(magikFile, range).stream(),
              this.typedChecksCodeActionProvider.provideCodeActions(magikFile, range).stream())
          .filter(
              codeAction ->
                  codeAction.getEdits().stream()
                      .anyMatch(edit -> edit.getRange().overlapsWith(range)))
          .toList();
    } catch (final IOException | ReflectiveOperationException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    // Safety.
    return Collections.emptyList();
  }
}
