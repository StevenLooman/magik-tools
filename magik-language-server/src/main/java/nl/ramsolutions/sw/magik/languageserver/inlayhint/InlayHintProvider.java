package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Provider for inlay hints.
 */
public class InlayHintProvider {

    /**
     * Set capabilities for inlay hints.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setInlayHintProvider(true);
    }

    /**
     * Provide inlay hints for the given file.
     * @param magikFile Magik file.
     * @param lsp4jrange Range in file.
     * @return List of inlay hints.
     */
    public List<InlayHint> provideInlayHints(final MagikTypedFile magikFile, final org.eclipse.lsp4j.Range lsp4jrange) {
        // Get argument hints from method invocations.
        final Range range = Lsp4jConversion.rangeFromLsp4j(lsp4jrange);
        final ArgumentInlayHintSupplier methodInvocationSupplier = new ArgumentInlayHintSupplier();
        final AtomInlayHintSupplier atomSupplier = new AtomInlayHintSupplier();
        return Stream.concat(
                methodInvocationSupplier.getMethodInvocationInlayHints(magikFile, range),
                atomSupplier.getAtomInlayHints(magikFile, range))
            // TODO: Sort on line/column?
            .collect(Collectors.toList());
    }

}
