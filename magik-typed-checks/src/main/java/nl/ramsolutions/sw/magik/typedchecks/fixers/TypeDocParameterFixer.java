package nl.ramsolutions.sw.magik.typedchecks.fixers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheckFixer;

/** TypeDoc parameter fixer. */
public class TypeDocParameterFixer extends MagikTypedCheckFixer {

  /**
   * Provide code actions related to parameter types.
   *
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
        .flatMap(methodDefinition -> this.extractParameterCodeActions(methodDefinition).stream())
        .toList();
  }

  private List<CodeAction> extractParameterCodeActions(final MethodDefinition methodDefinition) {
    // Compare and create TextEdits.
    return Stream.concat(
            this.createAddParameterTextEdits(methodDefinition).stream(),
            this.createRemoveParameterTextEdits(methodDefinition).stream())
        .toList();
  }

  private List<CodeAction> createAddParameterTextEdits(final MethodDefinition methodDefinition) {
    // Find all method and type-doc parameters.
    final List<ParameterDefinition> methodParameters =
        Stream.concat(
                methodDefinition.getParameters().stream(),
                Stream.ofNullable(methodDefinition.getAssignmentParameter()))
            .toList();
    final AstNode methodDefinitionNode = methodDefinition.getNode();
    Objects.requireNonNull(methodDefinitionNode);
    final TypeDocParser typeDocParser = new TypeDocParser(methodDefinitionNode);
    final Map<AstNode, String> typeDocParameters = typeDocParser.getParameterNameNodes();
    final Collection<String> typeDocParameterNames = typeDocParameters.values();

    // Find missing parameters, and generate CodeActions to add type-doc for these parameters.
    final int lastMethodDocLine = this.getLastMethodDocLine(methodDefinition);
    final int lastTypeDocParameterLine =
        typeDocParameters.keySet().stream()
            .mapToInt(AstNode::getTokenLine)
            .max()
            .orElse(lastMethodDocLine);
    return methodParameters.stream()
        .filter(paramDef -> !typeDocParameterNames.contains(paramDef.getName()))
        .map(
            paramDef -> {
              final Range range =
                  new Range(
                      new Position(lastTypeDocParameterLine + 1, 0),
                      new Position(lastTypeDocParameterLine + 1, 0));
              final String typeDocLine =
                  String.format("\t## @param {_undefined} %s Description%n", paramDef.getName());
              final String description =
                  String.format("Add type-doc for parameter %s", paramDef.getName());
              final TextEdit edit = new TextEdit(range, typeDocLine);
              return new CodeAction(description, edit);
            })
        .toList();
  }

  private List<CodeAction> createRemoveParameterTextEdits(final MethodDefinition methodDefinition) {
    // Find all parameters and type-doc parameters.
    final Set<String> methodParameterNames =
        Stream.concat(
                methodDefinition.getParameters().stream(),
                Stream.ofNullable(methodDefinition.getAssignmentParameter()))
            .map(ParameterDefinition::getName)
            .collect(Collectors.toSet());
    final AstNode methodDefinitionNode = methodDefinition.getNode();
    Objects.requireNonNull(methodDefinitionNode);
    final TypeDocParser typeDocParser = new TypeDocParser(methodDefinitionNode);
    final Map<AstNode, String> typeDocParameters = typeDocParser.getParameterNameNodes();

    return typeDocParameters.entrySet().stream()
        .filter(entry -> !methodParameterNames.contains(entry.getValue()))
        .map(
            entry -> {
              final AstNode paramNameNode = entry.getKey();
              final AstNode paramNode = paramNameNode.getParent();
              final Range treeRange = Range.fromTree(paramNode);
              final Range expandedRange =
                  new Range(
                      new Position(treeRange.getStartPosition().getLine(), 0),
                      new Position(treeRange.getEndPosition().getLine() + 1, 0));
              final String newText = "";
              final String description =
                  String.format("Remove type-doc for parameter %s", entry.getValue());
              final TextEdit edit = new TextEdit(expandedRange, newText);
              return new CodeAction(description, edit);
            })
        .toList();
  }

  private int getLastMethodDocLine(final MethodDefinition methodDefinition) {
    final AstNode methodDefinitionNode = methodDefinition.getNode();
    Objects.requireNonNull(methodDefinitionNode);
    final AstNode bodyNode = methodDefinitionNode.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      throw new IllegalStateException();
    }

    final Token bodyToken = bodyNode.getToken();
    if (bodyToken == null) {
      return methodDefinitionNode.getTokenLine();
    }

    final int bodyStart =
        bodyNode.getTokenLine() - 1; // Body starts at first method body token, so subtract 1.
    return MagikCommentExtractor.extractDocCommentTokens(methodDefinitionNode)
        .mapToInt(Token::getLine)
        .max()
        .orElse(bodyStart);
  }
}
