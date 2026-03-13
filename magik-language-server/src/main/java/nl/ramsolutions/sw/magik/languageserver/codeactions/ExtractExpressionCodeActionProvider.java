package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides "Extract to method" / "Extract expression to proc" refactoring code actions. */
public class ExtractExpressionCodeActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtractExpressionCodeActionProvider.class);

  private static final String PLACEHOLDER_METHOD_NAME = "extracted_method";
  private static final String PLACEHOLDER_PROC_NAME = "extracted_proc";

  /** Expression node types eligible for extraction. */
  private static final AstNodeType[] EXTRACTABLE_EXPRESSION_TYPES = {
    MagikGrammar.EXPRESSION,
    MagikGrammar.ASSIGNMENT_EXPRESSION,
    MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION,
    MagikGrammar.OR_EXPRESSION,
    MagikGrammar.XOR_EXPRESSION,
    MagikGrammar.AND_EXPRESSION,
    MagikGrammar.EQUALITY_EXPRESSION,
    MagikGrammar.RELATIONAL_EXPRESSION,
    MagikGrammar.ADDITIVE_EXPRESSION,
    MagikGrammar.MULTIPLICATIVE_EXPRESSION,
    MagikGrammar.EXPONENTIAL_EXPRESSION,
    MagikGrammar.UNARY_EXPRESSION,
    MagikGrammar.POSTFIX_EXPRESSION,
    MagikGrammar.ATOM,
  };

  /**
   * Statement types that make an expression non-extractable when they appear between the leaf node
   * and the candidate expression root. LOOPBODY is intentionally excluded so that expressions
   * inside a loop body can still be extracted.
   */
  private static final AstNodeType[] EXPRESSION_DISALLOWED_ANCESTOR_TYPES = {
    MagikGrammar.LEAVE_STATEMENT, MagikGrammar.CONTINUE_STATEMENT,
  };

  /**
   * Provide code actions for the given file and selection range.
   *
   * @param magikFile The typed Magik file.
   * @param range The selection range.
   * @return List of code actions, possibly empty.
   */
  public List<CodeAction> provideCodeActions(final MagikTypedFile magikFile, final Range range) {
    try {
      return this.doProvideCodeActions(magikFile, range);
    } catch (final Exception exception) {
      LOGGER.error("Error providing extract expression code actions", exception);
      return Collections.emptyList();
    }
  }

  private List<CodeAction> doProvideCodeActions(final MagikTypedFile magikFile, final Range range) {
    final AstNode topNode = magikFile.getTopNode();
    if (topNode == null) {
      return Collections.emptyList();
    }

    final ExpressionContext ctx = this.findExpressionContext(topNode, range);
    if (ctx == null) {
      return Collections.emptyList();
    }

    return this.buildExpressionCodeAction(magikFile, ctx);
  }

  // --- Context discovery ---

  /**
   * Walk up from {@code startNode} to find the tightest expression node that contains {@code
   * range}.
   */
  @CheckForNull
  private AstNode findTightestContainingExpression(final AstNode startNode, final Range range) {
    AstNode node = startNode;
    while (node != null) {
      if (node.is(EXTRACTABLE_EXPRESSION_TYPES) && Range.fromTree(node).contains(range)) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  /**
   * Returns {@code true} if any node on the path from {@code leaf} (inclusive) up to {@code root}
   * (exclusive) is a disallowed statement type such as LEAVE_STATEMENT or CONTINUE_STATEMENT.
   */
  private boolean isInsideDisallowedStatement(final AstNode leaf, final AstNode root) {
    AstNode node = leaf;
    while (node != null && node != root) {
      if (node.is(EXPRESSION_DISALLOWED_ANCESTOR_TYPES)) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  @CheckForNull
  private ExpressionContext findExpressionContext(final AstNode topNode, final Range range) {
    AstNode startNode = AstQuery.nodeAt(topNode, range.getStartPosition());
    if (startNode == null) {
      startNode = AstQuery.nodeSurrounding(topNode, range.getStartPosition());
    }
    if (startNode == null) {
      return null;
    }

    final AstNode exprNode = this.findTightestContainingExpression(startNode, range);
    if (exprNode == null) {
      return null;
    }

    if (this.isInsideDisallowedStatement(startNode, exprNode)) {
      return null;
    }

    final AstNode enclosingDef =
        AstQuery.getFirstAncestorOrSelf(
            exprNode,
            new AstNodeType[] {MagikGrammar.METHOD_DEFINITION},
            new AstNodeType[] {MagikGrammar.PROCEDURE_DEFINITION});
    if (enclosingDef == null) {
      return null;
    }

    final AstNode bodyNode = enclosingDef.getFirstDescendant(MagikGrammar.BODY);
    if (bodyNode == null) {
      return null;
    }

    return new ExpressionContext(enclosingDef, bodyNode, exprNode, range);
  }

  private record ExpressionContext(
      AstNode enclosingDef, AstNode bodyNode, AstNode expressionNode, Range selectedRange) {}

  // --- Action construction ---

  private List<CodeAction> buildExpressionCodeAction(
      final MagikTypedFile magikFile, final ExpressionContext ctx) {
    final List<ParameterInfo> params =
        this.analyzeExpressionParameters(magikFile, ctx.bodyNode(), ctx.selectedRange());
    if (params == null) {
      return Collections.emptyList();
    }

    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final TypeString returnType = this.getTypeStringForNode(ctx.expressionNode(), reasonerState);

    final boolean isMethodDef = ctx.enclosingDef().is(MagikGrammar.METHOD_DEFINITION);
    final CodeAction action =
        isMethodDef
            ? this.buildExtractExpressionToMethodAction(magikFile, ctx, params, returnType)
            : this.buildExtractExpressionToProcAction(magikFile, ctx, params, returnType);
    return action != null ? List.of(action) : Collections.emptyList();
  }

  // --- Parameter analysis ---

  @CheckForNull
  private List<ParameterInfo> analyzeExpressionParameters(
      final MagikTypedFile magikFile, final AstNode bodyNode, final Range selectedRange) {
    final GlobalScope globalScope = magikFile.getGlobalScope();
    if (globalScope == null) {
      return null;
    }
    final Scope procedureScope = globalScope.getScopeForNode(bodyNode);
    if (procedureScope == null) {
      return null;
    }

    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final Map<String, ParameterInfo> parameters = new LinkedHashMap<>();
    for (final ScopeEntry entry : this.collectScopeEntries(procedureScope)) {
      if (entry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC, ScopeEntry.Type.IMPORT)) {
        continue;
      }
      final AstNode defNode = entry.getDefinitionNode();
      if (defNode == null) {
        continue;
      }
      final Position defPos = Position.fromTokenStart(defNode.getToken());
      final boolean definedBefore = defPos.isBefore(selectedRange.getStartPosition());
      final boolean usedWithin =
          entry.getUsages().stream()
              .anyMatch(
                  u -> this.positionInRange(Position.fromTokenStart(u.getToken()), selectedRange));
      if (definedBefore && usedWithin) {
        final TypeString typeStr = this.getTypeForScopeEntry(entry, defNode, reasonerState);
        parameters.put(entry.getIdentifier(), new ParameterInfo(entry.getIdentifier(), typeStr));
      }
    }
    return new ArrayList<>(parameters.values());
  }

  @CheckForNull
  private TypeString getTypeStringForNode(
      final AstNode node, final LocalTypeReasonerState reasonerState) {
    try {
      final ExpressionResultString result = reasonerState.getNodeTypeSilent(node);
      if (result != null && !result.isEmpty()) {
        return result.get(0, TypeString.UNDEFINED);
      }
    } catch (final Exception exception) {
      LOGGER.debug("Could not resolve type for expression node", exception);
    }
    return null;
  }

  private record ParameterInfo(String name, @CheckForNull TypeString typeString) {}

  // --- Build: extract to method ---

  @CheckForNull
  private CodeAction buildExtractExpressionToMethodAction(
      final MagikTypedFile magikFile,
      final ExpressionContext ctx,
      final List<ParameterInfo> params,
      @CheckForNull final TypeString returnType) {
    final AstNode exemplarNameNode =
        ctx.enclosingDef().getFirstDescendant(MagikGrammar.EXEMPLAR_NAME);
    if (exemplarNameNode == null) {
      return null;
    }
    final String exemplarName = exemplarNameNode.getTokenValue();
    final String paramList =
        params.stream().map(ParameterInfo::name).collect(Collectors.joining(", "));
    final String indent = new MagikFormattingSettings(magikFile.getProperties()).getIndent();
    final String typeDoc = this.buildExpressionTypeDoc(params, returnType, indent);
    final String exprText = this.getSourceText(magikFile, ctx.selectedRange());
    if (exprText == null) {
      return null;
    }

    final String pragmaLine =
        ExtractUtils.getPragmaLineForPrivateMethod(magikFile, ctx.enclosingDef());
    final String newMethodText =
        this.buildNewExpressionMethodText(
            exemplarName, paramList, typeDoc, exprText, indent, pragmaLine);
    final String callText = buildCallText("_self." + PLACEHOLDER_METHOD_NAME, paramList, false);

    final Position insertionPosition = this.getMethodInsertionPosition(ctx.enclosingDef());
    final TextEdit replaceEdit = new TextEdit(ctx.selectedRange(), callText);
    final TextEdit insertEdit =
        new TextEdit(new Range(insertionPosition, insertionPosition), newMethodText);

    final int prefixLen = callText.indexOf(PLACEHOLDER_METHOD_NAME);
    final int renameLineNumber = ctx.selectedRange().getStartPosition().getLine();
    final int renameColumn = ctx.selectedRange().getStartPosition().getColumn() + prefixLen + 1;
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(magikFile.getUri().toString(), renameLineNumber, renameColumn));

    return new CodeAction(
        "Extract to method",
        List.of(replaceEdit, insertEdit),
        CodeAction.KIND_REFACTOR_EXTRACT,
        renameCommand);
  }

  // --- Build: extract to proc ---

  @CheckForNull
  private CodeAction buildExtractExpressionToProcAction(
      final MagikTypedFile magikFile,
      final ExpressionContext ctx,
      final List<ParameterInfo> params,
      @CheckForNull final TypeString returnType) {
    final String paramList =
        params.stream().map(ParameterInfo::name).collect(Collectors.joining(", "));
    final String indent = new MagikFormattingSettings(magikFile.getProperties()).getIndent();
    final String typeDoc = this.buildExpressionTypeDoc(params, returnType, indent);
    final String exprText = this.getSourceText(magikFile, ctx.selectedRange());
    if (exprText == null) {
      return null;
    }

    final String newProcText =
        this.buildNewExpressionProcText(paramList, typeDoc, exprText, indent);
    final String callText = buildCallText(PLACEHOLDER_PROC_NAME, paramList, true);

    final AstNode enclosingStmt =
        AstQuery.getFirstAncestor(ctx.expressionNode(), new AstNodeType[] {MagikGrammar.STATEMENT});
    final Position stmtStart =
        enclosingStmt != null
            ? Range.fromTree(enclosingStmt).getStartPosition()
            : ctx.selectedRange().getStartPosition();
    final String stmtIndent = this.getLineLeadingWhitespace(magikFile, stmtStart.getLine());
    final TextEdit replaceEdit = new TextEdit(ctx.selectedRange(), callText);
    final TextEdit insertEdit =
        new TextEdit(
            new Range(stmtStart, stmtStart),
            newProcText.lines().map(l -> stmtIndent + l).collect(Collectors.joining("\n"))
                + "\n"
                + stmtIndent);

    final long insertedLines = newProcText.lines().count();
    final int renameLineNumber =
        ctx.selectedRange().getStartPosition().getLine() + (int) insertedLines;
    final int prefixLen = callText.indexOf(PLACEHOLDER_PROC_NAME);
    final int renameColumn = ctx.selectedRange().getStartPosition().getColumn() + prefixLen + 1;
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted procedure",
            List.of(magikFile.getUri().toString(), renameLineNumber, renameColumn));

    return new CodeAction(
        "Extract expression to procedure",
        List.of(replaceEdit, insertEdit),
        CodeAction.KIND_REFACTOR_EXTRACT,
        renameCommand);
  }

  // --- Text builders ---

  private String buildNewExpressionMethodText(
      final String exemplarName,
      final String paramList,
      final String typeDoc,
      final String exprText,
      final String indent,
      @CheckForNull final String pragmaLine) {
    final StringBuilder sb = new StringBuilder();
    sb.append("\n");
    if (pragmaLine != null) {
      sb.append(pragmaLine).append("\n");
    }
    sb.append("_private _method ").append(exemplarName).append(".").append(PLACEHOLDER_METHOD_NAME);
    if (!paramList.isEmpty()) {
      sb.append("(").append(paramList).append(")");
    }
    sb.append("\n");
    if (!typeDoc.isEmpty()) {
      sb.append(typeDoc);
    }
    sb.append(indent).append("_return ").append(exprText.strip()).append("\n");
    sb.append("_endmethod\n$\n");
    return sb.toString();
  }

  private String buildNewExpressionProcText(
      final String paramList, final String typeDoc, final String exprText, final String indent) {
    final StringBuilder sb = new StringBuilder();
    sb.append("_local ")
        .append(PLACEHOLDER_PROC_NAME)
        .append(" << _proc(")
        .append(paramList)
        .append(")\n");
    if (!typeDoc.isEmpty()) {
      sb.append(typeDoc);
    }
    sb.append(indent.repeat(2)).append("_return ").append(exprText.strip()).append("\n");
    sb.append(indent).append("_endproc");
    return sb.toString();
  }

  private String buildExpressionTypeDoc(
      final List<ParameterInfo> params,
      @CheckForNull final TypeString returnType,
      final String indent) {
    final StringBuilder typeDoc = new StringBuilder();
    for (final ParameterInfo param : params) {
      if (param.typeString() != null && !param.typeString().isUndefined()) {
        typeDoc
            .append(indent)
            .append("## @param {")
            .append(param.typeString().getFullString())
            .append("} ")
            .append(param.name())
            .append("\n");
      }
    }
    if (returnType != null && !returnType.isUndefined()) {
      typeDoc
          .append(indent)
          .append("## @return {")
          .append(returnType.getFullString())
          .append("}\n");
    }
    return typeDoc.toString();
  }

  /**
   * Build the expression at the call-site that replaces the selected text.
   *
   * @param target The call target (e.g. "_self.extracted_method").
   * @param paramList Comma-separated parameter list (may be empty).
   * @param forceParens When {@code true}, always append "()" even for an empty param list.
   * @return The replacement text.
   */
  private static String buildCallText(
      final String target, final String paramList, final boolean forceParens) {
    if (forceParens || !paramList.isEmpty()) {
      return target + "(" + paramList + ")";
    }
    return target;
  }

  // --- Utility methods ---

  private List<ScopeEntry> collectScopeEntries(final Scope procedureScope) {
    final List<ScopeEntry> allEntries = new ArrayList<>();
    for (final Scope scope : procedureScope.getSelfAndDescendantScopes()) {
      allEntries.addAll(scope.getScopeEntriesInScope());
    }
    return allEntries;
  }

  private boolean positionInRange(final Position position, final Range range) {
    return position.compareTo(range.getStartPosition()) >= 0
        && position.compareTo(range.getEndPosition()) <= 0;
  }

  @CheckForNull
  private TypeString getTypeForScopeEntry(
      final ScopeEntry entry, final AstNode defNode, final LocalTypeReasonerState reasonerState) {
    try {
      final ExpressionResultString result = reasonerState.getNodeTypeSilent(defNode);
      if (result != null && !result.isEmpty()) {
        return result.get(0, TypeString.UNDEFINED);
      }
    } catch (final Exception exception) {
      LOGGER.debug("Could not resolve type for {}", entry.getIdentifier(), exception);
    }
    return null;
  }

  @CheckForNull
  private String getLineLeadingWhitespace(final MagikTypedFile magikFile, final int line) {
    final String source = magikFile.getSource();
    if (source == null) {
      return "";
    }
    final String[] lines = source.split("\n", -1);
    if (line <= 0 || line > lines.length) {
      return "";
    }
    final String srcLine = lines[line - 1];
    int i = 0;
    while (i < srcLine.length() && (srcLine.charAt(i) == ' ' || srcLine.charAt(i) == '\t')) {
      i++;
    }
    return srcLine.substring(0, i);
  }

  /**
   * Get the source text for a given range from the file.
   *
   * @param magikFile The Magik file.
   * @param range The range to extract.
   * @return The source text, or null if unable to extract.
   */
  @CheckForNull
  private String getSourceText(final MagikTypedFile magikFile, final Range range) {
    final String source = magikFile.getSource();
    if (source == null) {
      return null;
    }

    final String[] lines = source.split("\n", -1);
    final int startLine = range.getStartPosition().getLine() - 1; // 1-based to 0-based.
    final int endLine = range.getEndPosition().getLine() - 1;
    final int startCol = range.getStartPosition().getColumn();
    final int endCol = range.getEndPosition().getColumn();

    if (startLine < 0 || endLine >= lines.length) {
      return null;
    }

    if (startLine == endLine) {
      final String line = lines[startLine];
      final int safeEndCol = Math.min(endCol, line.length());
      return line.substring(startCol, safeEndCol);
    }

    final StringBuilder sb = new StringBuilder();
    sb.append(lines[startLine].substring(startCol));
    for (int i = startLine + 1; i < endLine; i++) {
      sb.append("\n").append(lines[i]);
    }
    final String lastLine = lines[endLine];
    final int safeEndCol = Math.min(endCol, lastLine.length());
    sb.append("\n").append(lastLine, 0, safeEndCol);
    return sb.toString();
  }

  /**
   * Get the position to insert the new method definition after the current method's TRANSMIT ($) or
   * after _endmethod.
   *
   * @param methodDefNode The METHOD_DEFINITION AST node.
   * @return The insertion position.
   */
  private Position getMethodInsertionPosition(final AstNode methodDefNode) {
    final AstNode nextSibling = methodDefNode.getNextSibling();
    if (nextSibling != null && nextSibling.is(MagikGrammar.TRANSMIT)) {
      final Range transmitRange = Range.fromTree(nextSibling);
      return new Position(transmitRange.getEndPosition().getLine() + 1, 0);
    }
    final Range methodRange = Range.fromTree(methodDefNode);
    return new Position(methodRange.getEndPosition().getLine() + 1, 0);
  }
}
