package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

/** Provides "Extract to method" / "Extract to proc" refactoring code actions. */
public class ExtractMethodCodeActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtractMethodCodeActionProvider.class);

  private static final String PLACEHOLDER_METHOD_NAME = "extracted_method";
  private static final String PLACEHOLDER_PROC_NAME = "extracted_proc";

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
      LOGGER.error("Error providing extract code actions", exception);
      return Collections.emptyList();
    }
  }

  private List<CodeAction> doProvideCodeActions(final MagikTypedFile magikFile, final Range range) {
    final AstNode topNode = magikFile.getTopNode();
    if (topNode == null) {
      return Collections.emptyList();
    }

    final EnclosingContext ctx = this.findEnclosingContext(topNode, range);
    if (ctx != null && !ExtractUtils.containsDisallowedNodes(ctx.selectedStatements())) {
      return this.buildCodeAction(magikFile, ctx);
    }

    return Collections.emptyList();
  }

  @CheckForNull
  private EnclosingContext findEnclosingContext(final AstNode topNode, final Range range) {
    // Find the shallowest BODY node (first in DFS order) that has at least one direct STATEMENT
    // child fully covered by the selection. "Shallowest first" ensures we extract from the
    // intended scope: e.g. selecting an _if block inside a _protect picks the _protect's body,
    // not the _if's own _then-body. Using range containment (not position probing) also handles
    // leading-whitespace selections where the start column is 0 before the indentation.
    AstNode bodyNode = null;
    List<AstNode> selectedStatements = Collections.emptyList();
    for (final AstNode node : AstQuery.dfs(topNode).toList()) {
      if (!node.is(MagikGrammar.BODY)) {
        continue;
      }
      final List<AstNode> stmts =
          node.getChildren(MagikGrammar.STATEMENT).stream()
              .filter(stmt -> range.contains(Range.fromTree(stmt)))
              .toList();
      if (!stmts.isEmpty()) {
        bodyNode = node;
        selectedStatements = stmts;
        break; // first (shallowest) match is the right scope
      }
    }
    if (bodyNode == null) {
      return null;
    }

    // Walk up to find the enclosing method/proc definition for this body.
    final AstNode enclosingDef =
        bodyNode.getFirstAncestor(
            MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    if (enclosingDef == null) {
      return null;
    }

    final Range selectedRange = this.combinedRange(selectedStatements);
    // The top-level BODY of the enclosing def is used for scope analysis: it covers all
    // variables visible in the method/proc, including those defined outside nested blocks.
    final AstNode scopeBodyNode = enclosingDef.getFirstDescendant(MagikGrammar.BODY);
    return new EnclosingContext(
        enclosingDef, bodyNode, scopeBodyNode, selectedStatements, selectedRange);
  }

  private record EnclosingContext(
      AstNode enclosingDef,
      AstNode bodyNode,
      AstNode scopeBodyNode,
      List<AstNode> selectedStatements,
      Range selectedRange) {}

  private List<CodeAction> buildCodeAction(
      final MagikTypedFile magikFile, final EnclosingContext ctx) {
    final VariableAnalysis analysis =
        this.analyzeVariables(
            magikFile, ctx.scopeBodyNode(), ctx.selectedStatements(), ctx.selectedRange());
    if (analysis == null) {
      return Collections.emptyList();
    }

    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    if (ctx.enclosingDef().is(MagikGrammar.METHOD_DEFINITION)) {
      return buildMethodDefActions(
          magikFile,
          ctx.enclosingDef(),
          ctx.selectedStatements(),
          ctx.selectedRange(),
          analysis,
          reasonerState);
    }
    final CodeAction procAction =
        this.buildProcAction(
            "Extract to procedure",
            magikFile,
            ctx.selectedStatements(),
            ctx.selectedRange(),
            analysis);
    return procAction != null ? List.of(procAction) : Collections.emptyList();
  }

  private List<CodeAction> buildMethodDefActions(
      final MagikTypedFile magikFile,
      final AstNode methodDefNode,
      final List<AstNode> selectedStatements,
      final Range selectedRange,
      final VariableAnalysis analysis,
      final LocalTypeReasonerState reasonerState) {
    return Stream.of(
            this.buildExtractMethodAction(
                magikFile,
                methodDefNode,
                selectedStatements,
                selectedRange,
                analysis,
                reasonerState),
            this.buildProcAction(
                "Extract to local procedure",
                magikFile,
                selectedStatements,
                selectedRange,
                analysis))
        .filter(a -> a != null)
        .toList();
  }

  private Range combinedRange(final List<AstNode> nodes) {
    final Range first = Range.fromTree(nodes.get(0));
    final Range last = Range.fromTree(nodes.get(nodes.size() - 1));
    return new Range(first.getStartPosition(), last.getEndPosition());
  }

  // --- Variable analysis ---

  /** Result of variable analysis. */
  private record VariableAnalysis(
      List<ParameterInfo> parameters, List<ReturnValueInfo> returnValues) {}

  private record ParameterInfo(String name, @CheckForNull TypeString typeString) {}

  private record ReturnValueInfo(String name, @CheckForNull TypeString typeString) {}

  @CheckForNull
  private VariableAnalysis analyzeVariables(
      final MagikTypedFile magikFile,
      final AstNode bodyNode,
      final List<AstNode> selectedStatements,
      final Range selectedRange) {
    final GlobalScope globalScope = magikFile.getGlobalScope();
    if (globalScope == null) {
      return null;
    }

    final Scope procedureScope = globalScope.getScopeForNode(bodyNode);
    if (procedureScope == null) {
      return null;
    }

    final List<ScopeEntry> allEntries = this.collectScopeEntries(procedureScope);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

    final Map<String, ParameterInfo> parameters = new LinkedHashMap<>();
    final Map<String, ReturnValueInfo> returnValues = new LinkedHashMap<>();

    for (final ScopeEntry entry : allEntries) {
      this.classifyEntry(entry, selectedRange, reasonerState, parameters, returnValues);
    }

    return new VariableAnalysis(
        new ArrayList<>(parameters.values()), new ArrayList<>(returnValues.values()));
  }

  private List<ScopeEntry> collectScopeEntries(final Scope procedureScope) {
    final List<ScopeEntry> allEntries = new ArrayList<>();
    for (final Scope scope : procedureScope.getSelfAndDescendantScopes()) {
      allEntries.addAll(scope.getScopeEntriesInScope());
    }
    return allEntries;
  }

  private void classifyEntry(
      final ScopeEntry entry,
      final Range selectedRange,
      final LocalTypeReasonerState reasonerState,
      final Map<String, ParameterInfo> parameters,
      final Map<String, ReturnValueInfo> returnValues) {
    if (entry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC, ScopeEntry.Type.IMPORT)) {
      return;
    }

    final AstNode defNode = entry.getDefinitionNode();
    if (defNode == null) {
      return;
    }

    final Position defPosition = Position.fromTokenStart(defNode.getToken());
    final boolean definedBeforeSelection = defPosition.isBefore(selectedRange.getStartPosition());
    final boolean definedInsideSelection = this.positionInRange(defPosition, selectedRange);

    final boolean usedInsideSelection =
        entry.getUsages().stream()
            .anyMatch(
                u -> this.positionInRange(Position.fromTokenStart(u.getToken()), selectedRange));
    final boolean usedAfterSelection =
        entry.getUsages().stream()
            .anyMatch(
                u -> Position.fromTokenStart(u.getToken()).isAfter(selectedRange.getEndPosition()));

    if (definedBeforeSelection && usedInsideSelection) {
      final TypeString typeStr = this.getTypeForScopeEntry(entry, defNode, reasonerState);
      parameters.put(entry.getIdentifier(), new ParameterInfo(entry.getIdentifier(), typeStr));
    }

    if (definedInsideSelection && usedAfterSelection) {
      final TypeString typeStr = this.getTypeForScopeEntry(entry, defNode, reasonerState);
      returnValues.put(entry.getIdentifier(), new ReturnValueInfo(entry.getIdentifier(), typeStr));
    }
  }

  private boolean positionInRange(final Position position, final Range range) {
    return position.compareTo(range.getStartPosition()) >= 0
        && position.compareTo(range.getEndPosition()) <= 0;
  }

  @CheckForNull
  private TypeString getTypeForScopeEntry(
      final ScopeEntry entry, final AstNode defNode, final LocalTypeReasonerState reasonerState) {
    try {
      // Try to get the type from the reasoner for the definition node.
      final ExpressionResultString result = reasonerState.getNodeTypeSilent(defNode);
      if (result != null && !result.isEmpty()) {
        return result.get(0, TypeString.UNDEFINED);
      }
    } catch (final Exception exception) {
      // Silently ignore type resolution failures.
      LOGGER.debug("Could not resolve type for {}", entry.getIdentifier(), exception);
    }
    return null;
  }

  // --- Extract to _method ---

  @CheckForNull
  private CodeAction buildExtractMethodAction(
      final MagikTypedFile magikFile,
      final AstNode methodDefNode,
      final List<AstNode> selectedStatements,
      final Range selectedRange,
      final VariableAnalysis analysis,
      final LocalTypeReasonerState reasonerState) {

    final AstNode exemplarNameNode = methodDefNode.getFirstDescendant(MagikGrammar.EXEMPLAR_NAME);
    if (exemplarNameNode == null) {
      return null;
    }
    final String exemplarName = exemplarNameNode.getTokenValue();

    final String paramList = this.buildParamList(analysis);
    final String indent = new MagikFormattingSettings(magikFile.getProperties()).getIndent();
    final String typeDoc = this.buildTypeDoc(analysis, indent);
    final String selectedText = this.getSourceText(magikFile, selectedRange);
    if (selectedText == null) {
      return null;
    }

    final String pragmaLine = ExtractUtils.getPragmaLineForPrivateMethod(magikFile, methodDefNode);
    final String newMethodText =
        this.buildNewMethodText(
            exemplarName, paramList, typeDoc, selectedText, analysis, indent, pragmaLine);
    final String callSiteText =
        this.buildCallSiteText("_self." + PLACEHOLDER_METHOD_NAME, paramList, analysis, false);

    final Position insertionPosition = this.getMethodInsertionPosition(methodDefNode);

    final TextEdit replaceEdit = new TextEdit(selectedRange, callSiteText);
    final TextEdit insertEdit =
        new TextEdit(new Range(insertionPosition, insertionPosition), newMethodText);

    // Compute the 1-based position of the placeholder identifier so that
    // magik.triggerRename can move the cursor to it and fire editor.action.rename.
    final int prefixLen = callSiteText.indexOf(PLACEHOLDER_METHOD_NAME);
    final int renameLineNumber = selectedRange.getStartPosition().getLine(); // already 1-based
    final int renameColumn =
        selectedRange.getStartPosition().getColumn() + prefixLen + 1; // 0->1-based
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

  private String buildParamList(final VariableAnalysis analysis) {
    return analysis.parameters().stream()
        .map(ParameterInfo::name)
        .collect(Collectors.joining(", "));
  }

  private String buildTypeDoc(final VariableAnalysis analysis, final String indent) {
    final StringBuilder typeDoc = new StringBuilder();
    for (final ParameterInfo param : analysis.parameters()) {
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
    for (final ReturnValueInfo rv : analysis.returnValues()) {
      if (rv.typeString() != null && !rv.typeString().isUndefined()) {
        typeDoc
            .append(indent)
            .append("## @return {")
            .append(rv.typeString().getFullString())
            .append("}\n");
      }
    }
    return typeDoc.toString();
  }

  @CheckForNull
  private String buildReturnValuesExpr(final List<ReturnValueInfo> returnValues) {
    if (returnValues.isEmpty()) {
      return null;
    }
    if (returnValues.size() == 1) {
      return returnValues.get(0).name();
    }
    return "("
        + returnValues.stream().map(ReturnValueInfo::name).collect(Collectors.joining(", "))
        + ")";
  }

  private String buildCallSiteText(
      final String callTarget,
      final String argList,
      final VariableAnalysis analysis,
      final boolean forceParens) {
    final StringBuilder callSite = new StringBuilder();
    final String retExpr = this.buildReturnValuesExpr(analysis.returnValues());
    if (retExpr != null) {
      callSite.append(retExpr).append(" << ");
    }
    callSite.append(callTarget);
    if (forceParens || !argList.isEmpty()) {
      callSite.append("(").append(argList).append(")");
    }
    return callSite.toString();
  }

  private String buildNewMethodText(
      final String exemplarName,
      final String paramList,
      final String typeDoc,
      final String selectedText,
      final VariableAnalysis analysis,
      final String indent,
      @CheckForNull final String pragmaLine) {
    final StringBuilder newMethod = new StringBuilder();
    newMethod.append("\n");
    if (pragmaLine != null) {
      newMethod.append(pragmaLine).append("\n");
    }
    newMethod.append("_private _method ").append(exemplarName).append(".");
    newMethod.append(PLACEHOLDER_METHOD_NAME);
    if (!paramList.isEmpty()) {
      newMethod.append("(").append(paramList).append(")");
    }
    newMethod.append("\n");
    if (!typeDoc.isEmpty()) {
      newMethod.append(typeDoc);
    }
    newMethod.append(this.reindent(selectedText, indent)).append("\n");
    final String retExpr = this.buildReturnValuesExpr(analysis.returnValues());
    if (retExpr != null) {
      newMethod.append(indent).append("_return ").append(retExpr).append("\n");
    }
    newMethod.append("_endmethod\n$\n");
    return newMethod.toString();
  }

  // --- Extract to _proc ---

  @CheckForNull
  private CodeAction buildProcAction(
      final String title,
      final MagikTypedFile magikFile,
      final List<AstNode> selectedStatements,
      final Range selectedRange,
      final VariableAnalysis analysis) {

    final String paramList = this.buildParamList(analysis);
    final String indent = new MagikFormattingSettings(magikFile.getProperties()).getIndent();
    final String typeDoc = this.buildTypeDoc(analysis, indent);
    final String selectedText = this.getSourceText(magikFile, selectedRange);
    if (selectedText == null) {
      return null;
    }

    final String newProcText =
        this.buildNewProcText(paramList, typeDoc, selectedText, analysis, indent);
    final String callSiteText =
        this.buildCallSiteText(PLACEHOLDER_PROC_NAME, paramList, analysis, true);

    final Range firstStmtRange = Range.fromTree(selectedStatements.get(0));
    final int stmtLine = firstStmtRange.getStartPosition().getLine();
    final String stmtIndent = ExtractUtils.getLineLeadingWhitespace(magikFile, stmtLine);
    final Position insertionPosition = new Position(stmtLine, 0);

    // Prefix every line with the statement's leading whitespace so the inserted block is
    // consistently indented in the document (VS Code inserts text literally; without the prefix
    // only the first line would be indented by the document context).
    final String procText = ExtractUtils.prefixLines(newProcText, stmtIndent);
    final TextEdit insertEdit =
        new TextEdit(new Range(insertionPosition, insertionPosition), procText);
    final TextEdit replaceEdit = new TextEdit(selectedRange, callSiteText);

    final long insertedLines = newProcText.chars().filter(c -> c == '\n').count();
    final int renameLineNumber =
        selectedRange.getStartPosition().getLine() + (int) insertedLines; // 1-based + shift
    final int prefixLen = callSiteText.indexOf(PLACEHOLDER_PROC_NAME);
    final int renameColumn =
        selectedRange.getStartPosition().getColumn() + prefixLen + 1; // 0->1-based
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted procedure",
            List.of(magikFile.getUri().toString(), renameLineNumber, renameColumn));

    return new CodeAction(
        title, List.of(replaceEdit, insertEdit), CodeAction.KIND_REFACTOR_EXTRACT, renameCommand);
  }

  private String buildNewProcText(
      final String paramList,
      final String typeDoc,
      final String selectedText,
      final VariableAnalysis analysis,
      final String indent) {
    final String bodyIndent = indent.repeat(2);
    final StringBuilder newProc = new StringBuilder();
    newProc.append("_local ").append(PLACEHOLDER_PROC_NAME).append(" << _proc(");
    newProc.append(paramList).append(")\n");
    if (!typeDoc.isEmpty()) {
      newProc.append(typeDoc);
    }
    newProc.append(this.reindent(selectedText, bodyIndent)).append("\n");
    final String retExpr = this.buildReturnValuesExpr(analysis.returnValues());
    if (retExpr != null) {
      newProc.append(bodyIndent).append("_return ").append(retExpr).append("\n");
    }
    newProc.append(indent).append("_endproc\n");
    return newProc.toString();
  }

  // --- Utility methods ---

  /**
   * Re-indent a (possibly multi-line) text block so that every line starts with {@code
   * targetIndent}, preserving relative indentation between lines.
   *
   * <p>The first line has already had its leading source-column whitespace removed by {@link
   * #getSourceText}; remaining lines still carry their original source indent. We determine the
   * common leading-whitespace prefix of lines 2+ and strip it before applying {@code targetIndent}
   * uniformly to all lines.
   *
   * @param text The raw selected text.
   * @param targetIndent The indent string to prepend to each non-blank line.
   * @return The re-indented text.
   */
  private String reindent(final String text, final String targetIndent) {
    final String[] rawLines = text.split("\n", -1);
    if (rawLines.length == 1) {
      return targetIndent + text.strip();
    }

    // Normalize leading tabs to spaces so mixed-indent sources are handled uniformly.
    final String[] lines = new String[rawLines.length];
    for (int i = 0; i < rawLines.length; i++) {
      lines[i] = this.expandLeadingTabs(rawLines[i]);
    }

    // Find the common leading-whitespace prefix among lines 2+ (non-blank only).
    String commonPrefix = null;
    for (int i = 1; i < lines.length; i++) {
      final String line = lines[i];
      if (line.isBlank()) {
        continue;
      }
      final int len = line.length() - line.stripLeading().length();
      final String leading = line.substring(0, len);
      commonPrefix = commonPrefix == null ? leading : this.commonPrefix(commonPrefix, leading);
    }
    if (commonPrefix == null) {
      commonPrefix = "";
    }

    final StringBuilder sb = new StringBuilder();
    // First line: already stripped to bare content by getSourceText().
    sb.append(targetIndent).append(lines[0].strip());
    for (int i = 1; i < lines.length; i++) {
      sb.append("\n");
      final String line = lines[i];
      if (line.isBlank()) {
        sb.append(line);
      } else {
        sb.append(targetIndent).append(line.substring(commonPrefix.length()));
      }
    }
    return sb.toString();
  }

  private String expandLeadingTabs(final String line) {
    int i = 0;
    final StringBuilder sb = new StringBuilder();
    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
      if (line.charAt(i) == '\t') {
        sb.append("    ");
      } else {
        sb.append(' ');
      }
      i++;
    }
    sb.append(line.substring(i));
    return sb.toString();
  }

  private String commonPrefix(final String a, final String b) {
    int k = 0;
    while (k < a.length() && k < b.length() && a.charAt(k) == b.charAt(k)) {
      k++;
    }
    return a.substring(0, k);
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
    // First line.
    sb.append(lines[startLine].substring(startCol));
    // Middle lines.
    for (int i = startLine + 1; i < endLine; i++) {
      sb.append("\n").append(lines[i]);
    }
    // Last line.
    final String lastLine = lines[endLine];
    final int safeEndCol = Math.min(endCol, lastLine.length());
    sb.append("\n").append(lastLine, 0, safeEndCol);
    return sb.toString();
  }

  /**
   * Get the position to insert the new method definition. This is after the TRANSMIT ($) node
   * following the current method definition, or after _endmethod if no TRANSMIT is found.
   *
   * @param methodDefNode The METHOD_DEFINITION AST node.
   * @return The insertion position.
   */
  private Position getMethodInsertionPosition(final AstNode methodDefNode) {
    // TRANSMIT is a sibling of METHOD_DEFINITION at the MAGIK root level.
    final AstNode nextSibling = methodDefNode.getNextSibling();
    if (nextSibling != null && nextSibling.is(MagikGrammar.TRANSMIT)) {
      // Insert after the TRANSMIT node.
      final Range transmitRange = Range.fromTree(nextSibling);
      return new Position(transmitRange.getEndPosition().getLine() + 1, 0);
    }

    // No TRANSMIT found; insert after _endmethod on the next line.
    final Range methodRange = Range.fromTree(methodDefNode);
    return new Position(methodRange.getEndPosition().getLine() + 1, 0);
  }
}
