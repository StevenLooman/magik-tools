package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.CodeAction;

/** "Extract to local variable" {@link CodeAction} module. */
class ExtractLocalVariableCodeActionModule implements CodeActionModule {

  private final ExtractLocalVariableCodeActionProvider provider =
      new ExtractLocalVariableCodeActionProvider();

  @Override
  public Stream<CodeAction> provideCodeActions(final CodeActionModuleContext context) {
    return this.provider.provideCodeActions(context.file(), context.range()).stream();
  }
}
