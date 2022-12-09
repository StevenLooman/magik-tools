package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.definitions.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeParser;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.NewDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;

/**
 * Test if referenced type is known.
 */
public class NewDocTypeExistsTypeCheck extends MagikTypedCheck {

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
        final NewDocParser newDocParser = new NewDocParser(node);
        final PackageNodeHelper packageNodeHelper = new PackageNodeHelper(node);
        final String pakkage = packageNodeHelper.getCurrentPackage();
        this.checkDefinitionParameters(newDocParser, pakkage);
        this.checkDefinitionLoops(newDocParser, pakkage);
        this.checkDefinitionReturns(newDocParser, pakkage);
    }

    private void checkDefinitionParameters(final NewDocParser newDocParser, final String pakkage) {
        // Test @param types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeParser typeParser = new TypeParser(typeKeeper);
        newDocParser.getParameterTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString);
                this.addIssue(typeNode, message);
            });
    }

    private void checkDefinitionLoops(final NewDocParser newDocParser, final String pakkage) {
        // Test @loop types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeParser typeParser = new TypeParser(typeKeeper);
        newDocParser.getLoopTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString);
                this.addIssue(typeNode, message);
            });
    }

    private void checkDefinitionReturns(final NewDocParser newDocParser, final String pakkage) {
        // Test @return types.
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeParser typeParser = new TypeParser(typeKeeper);
        newDocParser.getReturnTypeNodes().entrySet().stream()
            .filter(entry -> typeParser.parseTypeString(entry.getValue()) == UndefinedType.INSTANCE)
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString);
                this.addIssue(typeNode, message);
            });
    }

    @Override
    protected void walkPostProcedureInvocation(final AstNode node) {
        if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
            return;
        }

        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeParser typeParser = new TypeParser(typeKeeper);

        // Get slot defintions.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
        final NewDocParser newDocParser = new NewDocParser(statementNode);
        final Map<AstNode, TypeString> slotTypeNodes = newDocParser.getSlotTypeNodes();

        // Test slot types.
        slotTypeNodes.entrySet().stream()
            .filter(entry -> {
                final AbstractType type = typeParser.parseTypeString(entry.getValue());
                return type == UndefinedType.INSTANCE;
            })
            .forEach(entry -> {
                final AstNode typeNode = entry.getKey();
                final TypeString typeString = entry.getValue();
                final String message = String.format(MESSAGE, typeString);
                this.addIssue(typeNode, message);
            });
    }

}
