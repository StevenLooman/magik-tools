package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/**
 * Test if referenced type is known.
 */
@Rule(key = TypeDocTypeExistsTypedCheck.CHECK_KEY)
public class TypeDocTypeExistsTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "TypeDocTypeExists";

    private static final String MESSAGE = "Unknown type: %s";

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        this.checkMethodProcedureDefinition(node);
    }

    @Override
    protected void walkPostProcedureDefinition(final AstNode node) {
        this.checkMethodProcedureDefinition(node);
    }

    private void checkMethodProcedureDefinition(final AstNode node) {
        final TypeDocParser typeDocParser = new TypeDocParser(node);
        this.checkDefinitionParameters(typeDocParser);
        this.checkDefinitionLoops(typeDocParser);
        this.checkDefinitionReturns(typeDocParser);
    }

    private void checkDefinitionParameters(final TypeDocParser typeDocParser) {
        // Test @param types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeParser = new TypeReader(typeKeeper);
        typeDocParser.getParameterTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString.getFullString());
                this.addIssue(typeNode, message);
            });
    }

    private void checkDefinitionLoops(final TypeDocParser typeDocParser) {
        // Test @loop types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeParser = new TypeReader(typeKeeper);
        typeDocParser.getLoopTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString.getFullString());
                this.addIssue(typeNode, message);
            });
    }

    private void checkDefinitionReturns(final TypeDocParser typeDocParser) {
        // Test @return types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeParser = new TypeReader(typeKeeper);
        typeDocParser.getReturnTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString.getFullString());
                this.addIssue(typeNode, message);
            });
    }

    @Override
    protected void walkPostProcedureInvocation(final AstNode node) {
        if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
            return;
        }

        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeParser = new TypeReader(typeKeeper);

        // Get slot defintions.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
        final TypeDocParser typeDocParser = new TypeDocParser(statementNode);
        final Map<AstNode, TypeString> slotTypeNodes = typeDocParser.getSlotTypeNodes();

        // Test slot types.
        slotTypeNodes.entrySet().stream()
            .filter(entry -> {
                final AbstractType type = typeParser.parseTypeString(entry.getValue());
                return type == UndefinedType.INSTANCE;
            })
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString.getFullString());
                this.addIssue(typeNode, message);
            });
    }

}
