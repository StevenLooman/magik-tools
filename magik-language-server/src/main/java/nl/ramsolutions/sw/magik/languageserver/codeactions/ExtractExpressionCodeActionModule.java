package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.CodeAction;

/** "Extract expression to method" / "Extract expression to proc" {@link CodeAction} module. */
class ExtractExpressionCodeActionModule implements CodeActionModule {

  private final ExtractExpressionCodeActionProvider provider =
      new ExtractExpressionCodeActionProvider();

  @Override
  public Stream<CodeAction> provideCodeActions(final CodeActionModuleContext context) {
    return this.provider.provideCodeActions(context.file(), context.range()).stream();
  }
}
