package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.MagikTypedCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Code action provider. */
public class CodeActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CodeActionProvider.class);

  private final ChecksCodeActionProvider productCodeActionProvider;
  private final ChecksCodeActionProvider moduleDefCodeActionProvider;
  private final ChecksCodeActionProvider magikChecksCodeActionProvider;
  private final ChecksCodeActionProvider magikTypedChecksCodeActionProvider;

  /**
   * Constructor.
   *
   * @param properties {@link MagikToolsProperties} properties.
   */
  public CodeActionProvider(final MagikToolsProperties properties) {
    this.productCodeActionProvider =
        new ChecksCodeActionProvider(ProductDefCheckList.INSTANCE, properties);
    this.moduleDefCodeActionProvider =
        new ChecksCodeActionProvider(ModuleDefCheckList.INSTANCE, properties);
    this.magikChecksCodeActionProvider =
        new ChecksCodeActionProvider(MagikCheckList.INSTANCE, properties);
    this.magikTypedChecksCodeActionProvider =
        new ChecksCodeActionProvider(MagikTypedCheckList.INSTANCE, properties);
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
   * Provide code actions for {@link ProductDefFile}.
   *
   * @param openedFile {@link OpenedFile} product definition file.
   * @param range Range to provide code actions for.
   * @param context Code action context.
   * @return List of code actions.
   * @throws IOException -
   */
  public List<CodeAction> provideCodeActions(
      final OpenedFile openedFile, final Range range, final CodeActionContext context) {
    if (openedFile instanceof ProductDefFile productDefFile) {
      return this.provideCodeActions(productDefFile, range, context);
    } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
      return this.provideCodeActions(moduleDefFile, range, context);
    } else if (openedFile instanceof MagikTypedFile magikTypedFile) {
      return this.provideCodeActions(magikTypedFile, range, context);
    }

    throw new IllegalArgumentException(
        "Unsupported opened file type: " + openedFile.getClass().getName());
  }

  /**
   * Provide code actions for {@link ProductDefFile}.
   *
   * @param productDefFile {@link ProductDefFile} product definition file.
   * @param range Range to provide code actions for.
   * @param context Code action context.
   * @return List of code actions.
   * @throws IOException -
   */
  public List<CodeAction> provideCodeActions(
      final ProductDefFile productDefFile, final Range range, final CodeActionContext context) {
    try {
      return this.productCodeActionProvider.provideCodeActions(productDefFile, range).stream()
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

  /**
   * Provide code actions for {@link ModuleDefFile}.
   *
   * @param moduleDefFile Magik file.
   * @param range Range to provide code actions for.
   * @param context Code action context.
   * @return List of code actions.
   * @throws IOException -
   */
  public List<CodeAction> provideCodeActions(
      final ModuleDefFile moduleDefFile, final Range range, final CodeActionContext context) {
    try {
      return this.moduleDefCodeActionProvider.provideCodeActions(moduleDefFile, range).stream()
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

  /**
   * Provide code actions for {@link MagikFile}.
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
              this.magikChecksCodeActionProvider.provideCodeActions(magikFile, range).stream(),
              this.magikTypedChecksCodeActionProvider.provideCodeActions(magikFile, range).stream())
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
