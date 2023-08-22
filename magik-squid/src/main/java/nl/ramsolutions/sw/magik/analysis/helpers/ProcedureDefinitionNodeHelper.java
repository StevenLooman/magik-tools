package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for METHOD_DEFINITION nodes.
 */
public class ProcedureDefinitionNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public ProcedureDefinitionNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_DEFINITION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get parameters + nodes.
     * @return Map with parameters + PARAMETER nodes.
     */
    public Map<String, AstNode> getParameterNodes() {
        final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
        if (parametersNode == null) {
            return Collections.emptyMap();
        }

        return parametersNode
            .getChildren(MagikGrammar.PARAMETER).stream()
                .collect(Collectors.toMap(
                    parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue(),
                    parameterNode -> parameterNode));
    }

    /**
     * Get procedure name.
     * @return Name of the procedure.
     */
    public String getProcedureName() {
        final AstNode nameNode = node.getFirstChild(MagikGrammar.PROCEDURE_NAME);
        if (nameNode == null) {
            return ProcedureInstance.ANONYMOUS_PROCEDURE;
        }
        final AstNode labelNode = nameNode.getFirstChild(MagikGrammar.LABEL);
        return labelNode.getLastChild().getTokenOriginalValue();
    }

    public AstNode getProcedureNode() {
        return this.node.getChildren().stream()
            .filter(childNode -> childNode.isNot(MagikGrammar.values()))
            .findFirst()
            .orElseThrow();
    }

}
