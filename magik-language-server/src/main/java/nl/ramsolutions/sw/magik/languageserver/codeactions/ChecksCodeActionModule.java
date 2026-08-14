package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckList;
import nl.ramsolutions.sw.checks.CodeActionSupplier;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;

/**
 * {@link Check}-based {@link CodeAction} module, wrapping a {@link ChecksCodeActionProvider}.
 *
 * <p>Only contributes code actions whose edits overlap the requested range; the extract modules are
 * already scoped to the requested range by construction and intentionally do not filter this way.
 */
class ChecksCodeActionModule implements CodeActionModule {

  private final ChecksCodeActionProvider provider;

  ChecksCodeActionModule(
      final CheckList<? extends Check, ? extends CodeActionSupplier> checkList,
      final MagikToolsProperties properties) {
    this.provider = new ChecksCodeActionProvider(checkList, properties);
  }

  @Override
  public Stream<CodeAction> provideCodeActions(final CodeActionModuleContext context)
      throws IOException, ReflectiveOperationException {
    final Range range = context.range();
    return this.provider.provideCodeActions(context.file(), range).stream()
        .filter(
            codeAction ->
                codeAction.getEdits().stream()
                    .anyMatch(edit -> edit.getRange().overlapsWith(range)));
  }
}
