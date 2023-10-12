package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.utils.StreamUtils;
import org.sonar.check.Rule;

/**
 * Check to check if the @return types from method doc matches the reasoned return types.
 */
@Rule(key = MethodReturnMatchesDocTypedCheck.CHECK_KEY)
public class MethodReturnMatchesDocTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "MethodReturnMatchesDoc";

    private static final String MESSAGE = "@return type(s) (%s) do not match method return type(s) (%s).";

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        if (helper.isAbstractMethod()) {
            return;
        }

        final ExpressionResultString methodResultString = this.extractReasonedResult(node);
        final Map<AstNode, TypeString> typeDocNodes = this.extractMethodDocResult(node);
        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final TypeReader typeReader = new TypeReader(typeKeeper);
        StreamUtils.zip(methodResultString.stream(), typeDocNodes.entrySet().stream())
            .forEach(entry -> {
                if (entry.getKey() == null
                    || entry.getValue() == null) {
                    // Only bother with type-doc returns.
                    return;
                }

                final TypeString methodReturnTypeString = entry.getKey();
                final AbstractType methodReturnType = typeReader.parseTypeString(methodReturnTypeString);

                final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
                final TypeString docReturnTypeString = typeDocEntry.getValue();
                final AbstractType docReturnType = typeReader.parseTypeString(docReturnTypeString);

                if (!methodReturnType.equals(docReturnType)) {
                    final String message = String.format(
                        MESSAGE,
                        docReturnTypeString.getFullString(), methodReturnTypeString.getFullString());
                    final AstNode returnTypeNode = typeDocEntry.getKey();
                    final AstNode typeValueNode = returnTypeNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
                    this.addIssue(typeValueNode, message);
                }
            });
    }

    private ExpressionResultString extractReasonedResult(final AstNode node) {
        final LocalTypeReasoner reasoner = this.getReasoner();
        final ExpressionResult result = reasoner.getNodeType(node);
        return result.stream()
            .map(AbstractType::getTypeString)
            .collect(ExpressionResultString.COLLECTOR);
    }

    private Map<AstNode, TypeString> extractMethodDocResult(final AstNode node) {
        final TypeDocParser docParser = new TypeDocParser(node);
        return docParser.getReturnTypeNodes();
    }

}
