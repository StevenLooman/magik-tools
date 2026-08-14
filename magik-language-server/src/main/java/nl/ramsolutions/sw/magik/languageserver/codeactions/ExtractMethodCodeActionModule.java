package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.CodeAction;

/** "Extract to method" / "Extract to proc" {@link CodeAction} module. */
class ExtractMethodCodeActionModule implements CodeActionModule {

  private final ExtractMethodCodeActionProvider provider = new ExtractMethodCodeActionProvider();

  @Override
  public Stream<CodeAction> provideCodeActions(final CodeActionModuleContext context) {
    return this.provider.provideCodeActions(context.file(), context.range()).stream();
  }
}
