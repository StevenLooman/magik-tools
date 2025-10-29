package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckList;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.CodeActionSupplier;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;

/** Provide {@link CodeAction}s for {@link Check}s from {@link CheckList}. */
public class ChecksCodeActionProvider {

  final CheckList<? extends Check, ? extends CodeActionSupplier> checkList;
  final MagikToolsProperties properties;

  /**
   * Constructor.
   *
   * @param checkList {@link CheckList} of {@link Check}s to provide {@link CodeAction}s for.
   * @param properties {@link MagikToolsProperties} to use.
   */
  ChecksCodeActionProvider(
      final CheckList<? extends Check, ? extends CodeActionSupplier> checkList,
      final MagikToolsProperties properties) {
    this.checkList = checkList;
    this.properties = properties;
  }

  /**
   * Provide {@link CodeAction} for {@link Check}s.
   *
   * @param openedFile {@link OpenedFile} to check on.
   * @param range {@link Range} to get {@link CodeAction}s for.
   * @return List of {@link CodeAction}s.
   * @throws ReflectiveOperationException -
   * @throws IOException -
   */
  public List<CodeAction> provideCodeActions(final OpenedFile openedFile, final Range range)
      throws ReflectiveOperationException, IOException {
    final List<CodeAction> codeActions = new ArrayList<>();
    for (final Entry<Class<? extends Check>, List<Class<? extends CodeActionSupplier>>> entry :
        this.checkList.getBaseFixers().entrySet()) {
      final Class<? extends Check> checkClass = entry.getKey();
      final List<Class<? extends CodeActionSupplier>> fixerClasses = entry.getValue();
      for (final Class<? extends CodeActionSupplier> fixerClass : fixerClasses) {
        if (!this.isCheckEnabled(openedFile, checkClass)) {
          continue;
        }

        final CodeActionSupplier fixer =
            (CodeActionSupplier) fixerClass.getDeclaredConstructor().newInstance();
        List<CodeAction> fixerCodeActions = fixer.provideCodeActions(openedFile, range);
        codeActions.addAll(fixerCodeActions);
      }
    }
    return codeActions;
  }

  private boolean isCheckEnabled(final OpenedFile openedFile, final Class<?> checkClass)
      throws IOException {
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final MagikToolsProperties actualProperties =
        MagikToolsProperties.merge(this.properties, fileProperties);
    final List<Class<? extends Check>> checks = this.checkList.getBaseChecks();
    final ChecksConfiguration config = new ChecksConfiguration(checks, actualProperties);
    final List<CheckHolder> allChecks = config.getAllChecks();
    for (final CheckHolder checkHolder : allChecks) {
      if (checkHolder.getCheckClass().equals(checkClass)) {
        return checkHolder.isEnabled();
      }
    }

    // Check not found, so not enabled.
    return false;
  }
}
