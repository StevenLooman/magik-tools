package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;

/**
 * Check to check if the @return types from method doc matches the reasoned return types.
 */
public class MethodReturnMatchesDocTypedCheck extends MagikTypedCheck {

    private static final String MESSAGE = "@return type(s) (%s) do not match method return type(s) (%s).";

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        if (helper.isAbstractMethod()) {
            return;
        }

        final ExpressionResult docExpressionResult = this.extractMethodDocResult(node);
        final ExpressionResult reasonedReturnResult = this.extractReasonedResult(node);
        if (!docExpressionResult.equals(reasonedReturnResult)) {
            final String docTypesStr = docExpressionResult.getTypeNames(", ");
            final String reasonedTypesStr = reasonedReturnResult.getTypeNames(", ");
            final String message = String.format(MESSAGE, docTypesStr, reasonedTypesStr);
            this.addIssue(node, message);
        }
    }

    private ExpressionResult extractReasonedResult(final AstNode node) {
        final LocalTypeReasoner reasoner = this.getReasoner();
        ExpressionResult result = reasoner.getNodeTypeSilent(node);
        return Objects.requireNonNullElse(result, new ExpressionResult());
    }

    private ExpressionResult extractMethodDocResult(final AstNode node) {
        final TypeDocParser docParser = new TypeDocParser(node);
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeParser = new TypeReader(typeKeeper);
        final List<AbstractType> docReturnTypes = docParser.getReturnTypes().stream()
                .map(typeStr -> typeParser.parseTypeString(typeStr))
                .collect(Collectors.toList());
        return new ExpressionResult(docReturnTypes);
    }

}
