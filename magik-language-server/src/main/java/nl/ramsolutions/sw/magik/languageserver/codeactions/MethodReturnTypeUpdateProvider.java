package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jUtils;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.utils.StreamUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide a code action to update the @return type of a method.
 */
class MethodReturnTypeUpdateProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(MethodReturnTypeUpdateProvider.class);

    /**
     * Provide code actions related to method return type updates.
     * @param magikFile Magik file.
     * @param range Range to provide code actions for.
     * @param context Code action context.
     * @return List of code actions.
     */
    public List<Either<Command, CodeAction>> provideCodeActions(
            final MagikTypedFile magikFile,
            final org.eclipse.lsp4j.Range range,
            final CodeActionContext context) {
        return magikFile.getDefinitions().stream()
            .filter(MethodDefinition.class::isInstance)
            .map(MethodDefinition.class::cast)
            .filter(MethodDefinition::isActualMethodDefinition)
            .filter(methodDefinition -> Lsp4jUtils.rangeOverlaps(
                range,
                Lsp4jConversion.rangeToLsp4j(
                    nl.ramsolutions.sw.magik.analysis.Range.fromTree(methodDefinition.getNode()))))
            .flatMap(methodDefinition -> this.extractReturnTypeCodeActions(magikFile, methodDefinition).stream())
            .map(Either::<Command, CodeAction>forRight)
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
                if (methodTypeString == TypeString.UNDEFINED) {
                    // Don't propose code actions for undefined types.
                    return null;
                }

                final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
                if (methodTypeString != null && typeDocEntry == null) {
                    // Code action: Add type-doc line.
                    return this.createAddReturnCodeAction(magikFile, methodDefinition, methodTypeString);
                }

                final TypeString typeDocTypeString = typeDocEntry.getValue();
                final AstNode typeDocNode = typeDocEntry.getKey();
                final AstNode typeValueNode = typeDocNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
                if (methodTypeString == null && typeDocEntry != null) {
                    // Code action: Remove type-doc line.
                    return this.createRemoveReturnCodeAction(magikFile, typeValueNode);
                } else if (methodTypeString != null  // && typeDocTypeString != null
                    && methodTypeString != TypeString.UNDEFINED
                    && !methodTypeString.equals(typeDocTypeString)) {
                    // Code action: Update type-doc line.
                    return this.createUpdateReturnCodeAction(
                        magikFile, methodTypeString, typeValueNode);
                }

                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private CodeAction createUpdateReturnCodeAction(
            final MagikTypedFile magikFile,
            final TypeString methodTypeString,
            final AstNode typeValueNode) {
        final Range range = Range.fromTree(typeValueNode);
        final String methodTypeStringString = methodTypeString.getFullString();
        final String description = String.format("Update @return type to %s", methodTypeStringString);
        return Lsp4jUtils.createCodeAction(magikFile, range, methodTypeStringString, description);
    }

    private CodeAction createRemoveReturnCodeAction(
            final MagikTypedFile magikFile,
            final AstNode typeValueNode) {
        final AstNode typeDocReturnNode = typeValueNode.getParent();
        final Range treeRange = Range.fromTree(typeDocReturnNode);
        final Range expandedRange =
            new Range(
                new Position(treeRange.getStartPosition().getLine(), 0),
                new Position(treeRange.getEndPosition().getLine() + 1, 0));
        final String textEdit = "";
        final String description = "Remove @return type";
        return Lsp4jUtils.createCodeAction(magikFile, expandedRange, textEdit, description);
    }

    private CodeAction createAddReturnCodeAction(
            final MagikTypedFile magikFile,
            final MethodDefinition methodDefinition,
            final TypeString methodTypeString) {
        final int lastMethodDocLine = this.getLastMethodDocLine(methodDefinition);
        final Range range = new Range(
            new Position(lastMethodDocLine + 1, 0),
            new Position(lastMethodDocLine + 1, 0));
        final String textEdit = String.format("\t## @return {%s} Description%n", methodTypeString.getFullString());
        final String description = String.format("Add @return type %s", methodTypeString.getFullString());
        return Lsp4jUtils.createCodeAction(magikFile, range, textEdit, description);
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
        return MagikCommentExtractor.extractDocComments(methodDefinitionNode)
            .mapToInt(Token::getLine)
            .max()
            .orElse(bodyStart);
    }

}
