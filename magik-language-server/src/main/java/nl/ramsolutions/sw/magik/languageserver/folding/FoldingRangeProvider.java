package nl.ramsolutions.sw.magik.languageserver.folding;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Folding range provider.
 */
public class FoldingRangeProvider {

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setFoldingRangeProvider(true);
    }

    /**
     * Provide folding ranges.
     * @param magikFile Magik file.
     * @return {{FoldingRange}}s.
     */
    public List<FoldingRange> provideFoldingRanges(final MagikTypedFile magikFile) {
        // Parse and reason magik.
        AstNode topNode = magikFile.getTopNode();

        // Extract all BODY nodes and use these as folding ranges.
        return topNode.getDescendants(MagikGrammar.BODY).stream()
            .map(node -> {
                final AstNode parentNode = node.getParent();
                final AstNode folderNode = parentNode != null
                    ? parentNode
                    : node;
                final int startLine = folderNode.getTokenLine() - 1;
                final int endLine = folderNode.getLastToken().getLine() - 1;
                return new FoldingRange(startLine, endLine);
            })
            .collect(Collectors.toList());
    }

}
