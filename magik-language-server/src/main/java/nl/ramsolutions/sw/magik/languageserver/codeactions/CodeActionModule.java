package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.io.IOException;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.CodeAction;

/**
 * A single code action module. Contributes zero or more {@link CodeAction}s for a given context.
 * Unlike {@code HoverModule}/{@code DefinitionModule}/{@code ReferencesModule}, results from all
 * modules are combined, not just the first module's.
 */
public interface CodeActionModule {

  /**
   * Provide code actions for the given context.
   *
   * @param context Code action module context.
   * @return Code actions contributed by this module.
   * @throws IOException -
   * @throws ReflectiveOperationException -
   */
  Stream<CodeAction> provideCodeActions(CodeActionModuleContext context)
      throws IOException, ReflectiveOperationException;
}
