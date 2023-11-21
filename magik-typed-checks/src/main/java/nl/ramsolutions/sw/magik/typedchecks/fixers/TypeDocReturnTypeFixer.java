package nl.ramsolutions.sw.magik.typedchecks.fixers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheckFixer;
import nl.ramsolutions.sw.magik.utils.StreamUtils;

/**
 * TypeDoc return type fixer.
 */
public class TypeDocReturnTypeFixer extends MagikTypedCheckFixer {

    /**
     * Provide code actions related to return types.
     * @param magikFile Magik file.
     * @param range Range to provide code actions for.
     * @return List of code actions.
     */
    @Override
    public List<CodeAction> provideCodeActions(final MagikTypedFile magikFile, final Range range) {
        return magikFile.getDefinitions().stream()
            .filter(MethodDefinition.class::isInstance)
            .map(MethodDefinition.class::cast)
            .filter(MethodDefinition::isActualMethodDefinition)
            .filter(methodDef -> Range.fromTree(methodDef.getNode()).overlapsWith(range))
            .flatMap(methodDefinition -> this.extractReturnTypeCodeActions(magikFile, methodDefinition).stream())
            .collect(Collectors.toList());
    }

    private List<CodeAction> extractReturnTypeCodeActions(
            final MagikTypedFile magikFile,
            final MethodDefinition methodDefinition) {
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
        final AstNode methodDefinitionNode = methodDefinition.getNode();
        final ExpressionResult methodResult = reasoner.getNodeType(methodDefinitionNode);
        final ExpressionResultString methodResultString = methodResult.stream()
            .map(AbstractType::getTypeString)
            .collect(ExpressionResultString.COLLECTOR);

        final TypeDocParser typeDocParser = new TypeDocParser(methodDefinition.getNode());
        final Map<AstNode, TypeString> typeDocNodes = typeDocParser.getReturnTypeNodes();

        // Construct Code Actions.
        return StreamUtils.zip(methodResultString.stream(), typeDocNodes.entrySet().stream())
            .map(entry -> {
                final TypeString methodTypeString = entry.getKey();
                if (methodTypeString != null && methodTypeString.containsUndefined()) {
                    // Don't propose code actions for undefined types.
                    return null;
                }

                final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
                if (methodTypeString != null && typeDocEntry == null) {
                    return this.createAddReturnCodeAction(methodDefinition, methodTypeString);
                }

                final AstNode typeDocNode = typeDocEntry.getKey();
                final AstNode typeValueNode = typeDocNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
                if (methodTypeString == null && typeDocEntry != null) {
                    return this.createRemoveReturnCodeAction(typeValueNode);
                }

                if (typeDocEntry == null) {  // Keep checker happy.
                    return null;
                }

                final TypeString typeDocTypeString = typeDocEntry.getValue();
                if (methodTypeString != null  // && typeDocTypeString != null
                    && !methodTypeString.containsUndefined()
                    && !methodTypeString.equals(typeDocTypeString)) {
                    return this.createUpdateReturnCodeAction(methodTypeString, typeValueNode);
                }

                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private CodeAction createUpdateReturnCodeAction(
            final TypeString methodTypeString,
            final AstNode typeValueNode) {
        final Range range = Range.fromTree(typeValueNode);
        final String methodTypeStringString = methodTypeString.getFullString();
        final String description = String.format("Update @return type to %s", methodTypeStringString);
        final TextEdit edit = new TextEdit(range, methodTypeStringString);
        return new CodeAction(description, edit);
    }

    private CodeAction createRemoveReturnCodeAction(
            final AstNode typeValueNode) {
        final AstNode typeDocReturnNode = typeValueNode.getParent();
        final Range treeRange = Range.fromTree(typeDocReturnNode);
        final Range range =
            new Range(
                new Position(treeRange.getStartPosition().getLine(), 0),
                new Position(treeRange.getEndPosition().getLine() + 1, 0));
        final String textEdit = "";
        final String description = "Remove @return type";
        final TextEdit edit = new TextEdit(range, textEdit);
        return new CodeAction(description, edit);
    }

    private CodeAction createAddReturnCodeAction(
            final MethodDefinition methodDefinition,
            final TypeString methodTypeString) {
        final int lastMethodDocLine = this.getLastMethodDocLine(methodDefinition);
        final Range range = new Range(
            new Position(lastMethodDocLine + 1, 0),
            new Position(lastMethodDocLine + 1, 0));
        final String textEdit = String.format("\t## @return {%s} Description%n", methodTypeString.getFullString());
        final String description = String.format("Add @return type %s", methodTypeString.getFullString());
        final TextEdit edit = new TextEdit(range, textEdit);
        return new CodeAction(description, edit);
    }

    private int getLastMethodDocLine(final MethodDefinition methodDefinition) {
        final AstNode methodDefinitionNode = methodDefinition.getNode();
        final AstNode bodyNode = methodDefinitionNode.getFirstChild(MagikGrammar.BODY);
        if (bodyNode == null) {
            throw new IllegalStateException();
        }

        final Token bodyToken = bodyNode.getToken();
        if (bodyToken == null) {
            return methodDefinitionNode.getTokenLine();
        }

        final int bodyStart = bodyNode.getTokenLine() - 1;  // Body starts at first method body token, so subtract 1.
        return MagikCommentExtractor.extractDocCommentTokens(methodDefinitionNode)
            .mapToInt(Token::getLine)
            .max()
            .orElse(bodyStart);
    }

}
