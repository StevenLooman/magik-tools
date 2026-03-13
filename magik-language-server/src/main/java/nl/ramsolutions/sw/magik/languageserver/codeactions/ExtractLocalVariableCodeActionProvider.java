package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides "Extract to local variable" refactoring code actions. */
public class ExtractLocalVariableCodeActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtractLocalVariableCodeActionProvider.class);

  private static final String PLACEHOLDER_VARIABLE_NAME = "extracted_variable";

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
      LOGGER.error("Error providing extract local variable code actions", exception);
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

    final CodeAction action = this.buildExtractLocalVariableAction(magikFile, ctx);
    return action != null ? List.of(action) : Collections.emptyList();
  }

  // --- Context discovery ---

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

    return new ExpressionContext(enclosingDef, exprNode, range);
  }

  private record ExpressionContext(
      AstNode enclosingDef, AstNode expressionNode, Range selectedRange) {}

  // --- Action construction ---

  @CheckForNull
  private CodeAction buildExtractLocalVariableAction(
      final MagikTypedFile magikFile, final ExpressionContext ctx) {
    final String exprText = this.getSourceText(magikFile, ctx.selectedRange());
    if (exprText == null) {
      return null;
    }

    final AstNode enclosingStmt =
        AstQuery.getFirstAncestor(ctx.expressionNode(), new AstNodeType[] {MagikGrammar.STATEMENT});
    final Position stmtStart =
        enclosingStmt != null
            ? Range.fromTree(enclosingStmt).getStartPosition()
            : ctx.selectedRange().getStartPosition();
    final String stmtIndent = this.getLineLeadingWhitespace(magikFile, stmtStart.getLine());

    // "_local extracted_variable << <expr>\n<stmtIndent>" inserted before the statement
    final String insertText =
        "_local " + PLACEHOLDER_VARIABLE_NAME + " << " + exprText.strip() + "\n" + stmtIndent;
    final TextEdit insertEdit = new TextEdit(new Range(stmtStart, stmtStart), insertText);

    // Replace the expression with just the variable name
    final TextEdit replaceEdit = new TextEdit(ctx.selectedRange(), PLACEHOLDER_VARIABLE_NAME);

    // The rename target is the variable reference that replaced the expression.
    // After the insert edit the statement line shifts down by 1, so we add 1 to the line.
    final int renameLineNumber = ctx.selectedRange().getStartPosition().getLine() + 1;
    final int renameColumn = ctx.selectedRange().getStartPosition().getColumn() + 1;
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted variable",
            List.of(magikFile.getUri().toString(), renameLineNumber, renameColumn));

    return new CodeAction(
        "Extract to local variable",
        List.of(replaceEdit, insertEdit),
        CodeAction.KIND_REFACTOR_EXTRACT,
        renameCommand);
  }

  // --- Utility methods ---

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
}
