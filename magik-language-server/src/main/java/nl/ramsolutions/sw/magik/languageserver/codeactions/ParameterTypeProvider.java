package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jUtils;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Class to provide code actions for parameter type information.
 */
public class ParameterTypeProvider {

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
                    Range.fromTree(methodDefinition.getNode()))))
            .flatMap(methodDefinition -> this.extractParameterCodeActions(magikFile, methodDefinition).stream())
            .map(Either::<Command, CodeAction>forRight)
            .collect(Collectors.toList());
    }

    private List<CodeAction> extractParameterCodeActions(
            final MagikTypedFile magikFile,
            final MethodDefinition methodDefinition) {
        // Compare and create Code Actions.
        return Stream.concat(
                this.createAddParameterCodeAction(magikFile, methodDefinition).stream(),
                this.createRemoveParameterCodeAction(magikFile, methodDefinition).stream())
            .collect(Collectors.toList());
    }

    private Collection<CodeAction> createAddParameterCodeAction(
            final MagikTypedFile magikFile,
            final MethodDefinition methodDefinition) {
        // Find all method and type-doc parameters.
        final List<ParameterDefinition> methodParameters = Stream.concat(
                methodDefinition.getParameters().stream(),
                Stream.ofNullable(methodDefinition.getAssignmentParameter()))
            .collect(Collectors.toList());
        final TypeDocParser typeDocParser = new TypeDocParser(methodDefinition.getNode());
        final Map<AstNode, String> typeDocParameters = typeDocParser.getParameterNameNodes();
        final Collection<String> typeDocParameterNames = typeDocParameters.values();

        // Find missing parameters, and generate CodeActions to add type-doc for these parameters.
        final int lastMethodDocLine = this.getLastMethodDocLine(methodDefinition);
        final int lastTypeDocParameterLine = typeDocParameters.keySet().stream()
            .mapToInt(AstNode::getTokenLine)
            .max()
            .orElse(lastMethodDocLine);
        return methodParameters.stream()
            .filter(paramDef -> !typeDocParameterNames.contains(paramDef.getName()))
            .map(paramDef -> {
                final Range range = new Range(
                    new Position(lastTypeDocParameterLine + 1, 0),
                    new Position(lastTypeDocParameterLine + 1, 0));
                final String typeDocLine =
                    String.format("\t## @param {_undefined} %s Description%n", paramDef.getName());
                final String description = String.format("Add type-doc for parameter %s", paramDef.getName());
                return Lsp4jUtils.createCodeAction(magikFile, range, typeDocLine, description);
            })
            .collect(Collectors.toList());
    }

    private Collection<CodeAction> createRemoveParameterCodeAction(
            final MagikTypedFile magikFile,
            final MethodDefinition methodDefinition) {
        // Find all parameters and type-doc parameters.
        final Set<String> methodParameterNames = Stream.concat(
                methodDefinition.getParameters().stream(),
                Stream.ofNullable(methodDefinition.getAssignmentParameter()))
            .map(ParameterDefinition::getName)
            .collect(Collectors.toSet());
        final TypeDocParser typeDocParser = new TypeDocParser(methodDefinition.getNode());
        final Map<AstNode, String> typeDocParameters = typeDocParser.getParameterNameNodes();

        return typeDocParameters.entrySet().stream()
            .filter(entry -> !methodParameterNames.contains(entry.getValue()))
            .map(entry -> {
                final AstNode paramNameNode = entry.getKey();
                final AstNode paramNode = paramNameNode.getParent();
                final Range treeRange = Range.fromTree(paramNode);
                final Range expandedRange =
                    new Range(
                        new Position(treeRange.getStartPosition().getLine(), 0),
                        new Position(treeRange.getEndPosition().getLine() + 1, 0));
                final String newText = "";
                final String description = String.format("Remove type-doc for parameter %s", entry.getValue());
                return Lsp4jUtils.createCodeAction(magikFile, expandedRange, newText, description);
            })
            .collect(Collectors.toList());
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
