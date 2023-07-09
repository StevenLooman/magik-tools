package nl.ramsolutions.sw.magik.languageserver.codeactions;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Code action provider.
 */
public class CodeActionProvider {

    private final MethodReturnTypeUpdateProvider methodReturnTypeUpdateProvider = new MethodReturnTypeUpdateProvider();
    private final ParameterTypeProvider parameterTypeProvider = new ParameterTypeProvider();

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setCodeActionProvider(true);
    }

    /**
     * Provide code actions.
     * @param magikFile Magik file.
     * @param range Range to provide code actions for.
     * @param context Code action context.
     * @return List of code actions.
     */
    public List<Either<Command, CodeAction>> provideCodeActions(
            final MagikTypedFile magikFile,
            final Range range,
            final CodeActionContext context) {
        return Stream.concat(
                this.methodReturnTypeUpdateProvider.provideCodeActions(magikFile, range, context).stream(),
                this.parameterTypeProvider.provideCodeActions(magikFile, range, context).stream()
            )
            .collect(Collectors.toList());
    }

}
