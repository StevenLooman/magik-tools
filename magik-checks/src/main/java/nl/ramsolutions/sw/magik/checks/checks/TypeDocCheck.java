package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/**
 * Check TypeDoc.
 */
@Rule(key = TypeDocCheck.CHECK_KEY)
public class TypeDocCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "TypeDoc";

    private static final String MESSAGE_PARAM_MISSING = "Missing parameter: %s.";
    private static final String MESSAGE_PARAM_UNKNOWN = "Unknown parameter: %s.";
    private static final String MESSAGE_SLOT_MISSING = "Missing slot: %s.";
    private static final String MESSAGE_SLOT_UNKNOWN = "Unknown slot: %s.";

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        this.checkDefinitionParameters(node);
    }

    @Override
    protected void walkPostProcedureDefinition(final AstNode node) {
        this.checkDefinitionParameters(node);
    }

    private void checkDefinitionParameters(final AstNode node) {
        // Get parameter defintions.
        final TypeDocParser typeDocParser = new TypeDocParser(node);
        final Map<AstNode, String> docParameterNameNodes = typeDocParser.getParameterNameNodes();
        final Map<String, AstNode> parameterNodes = this.getParameterNodes(node);

        // Compare parameters.
        docParameterNameNodes.entrySet().stream()
            .filter(entry -> !parameterNodes.containsKey(entry.getValue()))
            .forEach(entry -> {
                final String docName = entry.getValue();
                final AstNode docNode = entry.getKey();
                final String message = String.format(MESSAGE_PARAM_UNKNOWN, docName);
                this.addIssue(docNode, message);
            });

        parameterNodes.entrySet().stream()
            .filter(entry -> !docParameterNameNodes.containsValue(entry.getKey()))
            .forEach(entry -> {
                final String docName = entry.getKey();
                final AstNode docNode = entry.getValue().getFirstDescendant(MagikGrammar.IDENTIFIER);
                final String message = String.format(MESSAGE_PARAM_MISSING, docName);
                this.addIssue(docNode, message);
            });
    }

    private Map<String, AstNode> getParameterNodes(final AstNode node) {
        if (node.is(MagikGrammar.METHOD_DEFINITION)) {
            final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
            return helper.getParameterNodes();
        }

        final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
        return helper.getParameterNodes();
    }

    @Override
    protected void walkPostProcedureInvocation(AstNode node) {
        if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
            return;
        }

        // Get slot defintions.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
        final TypeDocParser typeDocParser = new TypeDocParser(statementNode);
        final Map<String, AstNode> docSlotNameNodes = typeDocParser.getSlotNameNodes();

        final DefSlottedExemplarParser parser = new DefSlottedExemplarParser(node);
        final List<Definition> definitions = parser.parseDefinitions();
        final ExemplarDefinition exemplarDefinition = (ExemplarDefinition) definitions.get(0);
        final List<SlotDefinition> slots = exemplarDefinition.getSlots();
        final Map<String, SlotDefinition> slotNames = slots.stream()
            .collect(Collectors.toMap(
                SlotDefinition::getName,
                slot -> slot));

        // Compare parameters.
        docSlotNameNodes.entrySet().stream()
            .filter(entry -> !slotNames.containsKey(entry.getKey()))
            .forEach(entry -> {
                final String docName = entry.getKey();
                final AstNode docNode = entry.getValue();
                final String message = String.format(MESSAGE_SLOT_UNKNOWN, docName);
                this.addIssue(docNode, message);
            });

        slotNames.entrySet().stream()
            .filter(entry -> !docSlotNameNodes.containsKey(entry.getKey()))
            .forEach(entry -> {
                final String docName = entry.getKey();
                final AstNode docNode = entry.getValue().getNode();
                final String message = String.format(MESSAGE_SLOT_MISSING, docName);
                this.addIssue(docNode, message);
            });
    }

}
