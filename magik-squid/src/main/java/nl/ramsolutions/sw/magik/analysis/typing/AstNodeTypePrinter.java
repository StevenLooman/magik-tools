package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.io.Writer;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;

/**
 * AstNode + assigned types printer.
 * Useful for debugging the LocalTypeReasoner.
 */
public final class AstNodeTypePrinter {

    private final LocalTypeReasonerState reasonerState;
    private final Writer writer;

    private AstNodeTypePrinter(final LocalTypeReasonerState reasonerState, final Writer writer) {
        this.reasonerState = reasonerState;
        this.writer = writer;
    }

    /**
     * Print the AstNodes and its types.
     * @param reasonerState ReasonerState to get types from.
     * @param rootNode Node to print.
     * @param writer Writer to write to.
     */
    public static void print(final LocalTypeReasonerState reasonerState, final AstNode rootNode, final Writer writer) {
        final AstNodeTypePrinter printer = new AstNodeTypePrinter(reasonerState, writer);
        try {
            printer.print(0, rootNode);
        } catch (IOException exception) {
            exception.printStackTrace();  // NOSONAR: Debug tooling only.
        }
    }

    private void print(final int level, final AstNode node) throws IOException {
        if (level != 0) {
            writer.append("\n");
        }

        // Indent.
        this.writer.append(" ".repeat(level * 2));

        final ExpressionResult result = this.reasonerState.getNodeTypeSilent(node);
        this.writer.append(
            node.getName() + ", "
            + "token: \"" + node.getTokenValue() + "\"" + ", "
            + "type: " + result);
        if (node.hasChildren()) {
            for (final AstNode childNode : node.getChildren()) {
                this.print(level + 1, childNode);
            }
        }
    }

}
