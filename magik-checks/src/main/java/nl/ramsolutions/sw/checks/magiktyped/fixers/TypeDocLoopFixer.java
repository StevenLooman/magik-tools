package nl.ramsolutions.sw.checks.magiktyped.fixers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.MagikTypedCodeActionSupplier;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.TypeDocGrammar;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.utils.StreamUtils;

/** TypeDoc @loop fixer. */
public class TypeDocLoopFixer extends MagikTypedCodeActionSupplier {

  /**
   * Provide code actions related to loop types.
   *
   * @param magikFile Magik file.
   * @param range Range to provide code actions for.
   * @return List of code actions.
   */
  @Override
  public List<CodeAction> provideMagikTypedCodeActions(
      final MagikTypedFile magikFile, final Range range) {
    final MagikToolsProperties properties = magikFile.getProperties();
    final MagikFormattingSettings settings = new MagikFormattingSettings(properties);
    final String indent = settings.getIndent();
    return magikFile.getMagikDefinitions().stream()
        .filter(
            magikDefinition ->
                magikDefinition instanceof MethodDefinition
                    || magikDefinition instanceof ProcedureDefinition)
        .map(magikDefinition -> magikDefinition.getNode())
        .filter(Objects::nonNull)
        .filter(node -> node.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION))
        .filter(node -> Range.fromTree(node).overlapsWith(range))
        .flatMap(node -> this.extractLoopTypeCodeActions(magikFile, node, indent).stream())
        .toList();
  }

  private List<CodeAction> extractLoopTypeCodeActions(
      final MagikTypedFile magikFile, final AstNode callableNode, final String indent) {
    final TypeDocParser typeDocParser = new TypeDocParser(callableNode);
    final Map<AstNode, TypeString> typeDocNodes = typeDocParser.getLoopTypeNodes();
    if (!this.isIterCallable(callableNode)) {
      return typeDocNodes.keySet().stream().map(this::createRemoveLoopCodeAction).toList();
    }

    if (this.isAbstractCallable(callableNode)) {
      return List.of();
    }

    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeIterType(callableNode);

    return StreamUtils.zip(result.stream(), typeDocNodes.entrySet().stream())
        .map(
            entry -> {
              final TypeString callableLoopType = entry.getKey();
              if (callableLoopType != null && callableLoopType.containsUndefined()) {
                // Don't propose code actions for undefined types.
                return null;
              }

              final Map.Entry<AstNode, TypeString> typeDocEntry = entry.getValue();
              if (callableLoopType != null && typeDocEntry == null) {
                return this.createAddLoopCodeAction(callableNode, indent, callableLoopType);
              }

              if (typeDocEntry == null) { // Keep checker happy.
                return null;
              }

              final AstNode typeDocNode = typeDocEntry.getKey();
              final AstNode typeValueNode = typeDocNode.getFirstChild(TypeDocGrammar.TYPE_VALUE);
              if (callableLoopType == null) {
                return this.createRemoveLoopCodeAction(typeValueNode);
              }

              final TypeString typeDocTypeString = typeDocEntry.getValue();
              if (!callableLoopType.equals(typeDocTypeString)) {
                return this.createUpdateLoopCodeAction(callableLoopType, typeValueNode);
              }

              return null;
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private CodeAction createUpdateLoopCodeAction(
      final TypeString callableLoopType, final AstNode typeValueNode) {
    final Range range = Range.fromTree(typeValueNode);
    final String callableLoopTypeString = callableLoopType.getFullString();
    final String description = "Update @loop type to %s".formatted(callableLoopTypeString);
    final TextEdit edit = new TextEdit(range, callableLoopTypeString);
    return new CodeAction(description, edit);
  }

  private CodeAction createRemoveLoopCodeAction(final AstNode typeValueNode) {
    final AstNode typeDocLoopNode = typeValueNode.getParent();
    final Range treeRange = Range.fromTree(typeDocLoopNode);
    final Range range =
        new Range(
            new Position(treeRange.getStartPosition().getLine(), 0),
            new Position(treeRange.getEndPosition().getLine() + 1, 0));
    final String textEdit = "";
    final String description = "Remove @loop type";
    final TextEdit edit = new TextEdit(range, textEdit);
    return new CodeAction(description, edit);
  }

  private CodeAction createAddLoopCodeAction(
      final AstNode callableNode, final String indent, final TypeString callableLoopType) {
    final int lastCallableDocLine = this.getLastCallableDocLine(callableNode);
    final Range range =
        new Range(
            new Position(lastCallableDocLine + 1, 0), new Position(lastCallableDocLine + 1, 0));
    final String textEdit =
        "%s## @loop {%s} Description%n".formatted(indent, callableLoopType.getFullString());
    final String description = "Add @loop type %s".formatted(callableLoopType.getFullString());
    final TextEdit edit = new TextEdit(range, textEdit);
    return new CodeAction(description, edit);
  }

  private int getLastCallableDocLine(final AstNode callableNode) {
    final AstNode bodyNode = callableNode.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      throw new IllegalStateException();
    }

    final Token bodyToken = bodyNode.getToken();
    if (bodyToken == null) {
      return callableNode.getTokenLine();
    }

    final int bodyStart =
        bodyNode.getTokenLine() - 1; // Body starts at first method body token, so subtract 1.
    return MagikCommentExtractor.extractDocCommentTokens(callableNode)
        .mapToInt(Token::getLine)
        .max()
        .orElse(bodyStart);
  }

  private boolean isIterCallable(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.isIterMethod();
    }

    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    return helper.isIterProc();
  }

  private boolean isAbstractCallable(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
      return helper.isAbstractMethod();
    }

    return false;
  }
}
