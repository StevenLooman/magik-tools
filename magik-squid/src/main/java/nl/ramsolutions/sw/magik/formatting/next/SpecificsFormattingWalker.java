package nl.ramsolutions.sw.magik.formatting.next;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

/** AST walker for formatting arguments and parameters. */
class SpecificsFormattingWalker extends FormattingWalker2 {

  SpecificsFormattingWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkPostParameter(final AstNode node) {
    this.handleArgumentParameter(node);
  }

  @Override
  protected void walkPostArgument(final AstNode node) {
    this.handleArgumentParameter(node);
  }

  private void handleArgumentParameter(final AstNode node) {
    final Token token = node.getToken();
    final AstNode previousNode = node.getPreviousSibling();
    final Token previousToken = previousNode != null ? previousNode.getToken() : null;

    if (AstQuery.tokenIs(
        previousToken, MagikPunctuator.PAREN_L.getValue(), MagikPunctuator.SQUARE_L.getValue())) {
      // No leading whitespace before the first argument/parameter.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(token);
    } else {
      // Leading whitespace before N>0 argument/parameter.
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(token);
    }
  }

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final AstNode exemplarNameNode = node.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
    if (exemplarNameNode != null) {
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(exemplarNameNode);
    }

    final AstNode methodNameNode = node.getFirstChild(MagikGrammar.METHOD_NAME);
    if (methodNameNode != null) {
      // Ensure no whitespace before the method name token.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(methodNameNode);
    }

    final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(parametersNode);
    }

    final Token assignmentToken =
        AstQuery.getFirstChildToken(
            node, MagikOperator.CHEVRON.getValue(), MagikOperator.BOOT_CHEVRON.getValue());
    if (assignmentToken != null) {
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(assignmentToken);

      // Assignment node is handled by regular ARGUMENT node.
    }
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    final AstNode procedureNameNode = node.getFirstChild(MagikGrammar.PROCEDURE_NAME);
    if (procedureNameNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(procedureNameNode);
    }
  }

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Ensure no whitespace before the `.` or `[` token.
    final List<AstNode> childNodes = node.getChildren();
    final AstNode dotOrSquareNode = childNodes.get(0);
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(dotOrSquareNode);

    // Ensure no whitespace before the method name token.
    final AstNode methodNameNode = node.getFirstChild(MagikGrammar.METHOD_NAME);
    if (methodNameNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(methodNameNode);
    }

    // Ensure no whitespace before the `(`/`[` token.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(argumentsNode);
    }

    // Ensure no whitespace before the `<<` token.
    final Token assignmentToken =
        AstQuery.getFirstChildToken(
            node, MagikOperator.CHEVRON.getValue(), MagikOperator.BOOT_CHEVRON.getValue());
    if (assignmentToken != null) {
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(assignmentToken);

      // Assignment node is handled by regular ARGUMENT node.
    }
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    // Ensure no whitespace before the `(` token.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    this.ensureNoWhitespaceBefore(argumentsNode);
  }

  @Override
  protected void walkPostMultipleAssignmentStatement(final AstNode node) {
    final AstNode multipleAssignmentAssignables =
        node.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(multipleAssignmentAssignables);

    final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(tupleNode);
  }

  @Override
  protected void walkPostVariableDefinitionStatement(final AstNode node) {
    final AstNode variableDefinitonMultiNode =
        node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MULTI);
    if (variableDefinitonMultiNode != null) {
      final AstNode identifiersWithGatherNode =
          variableDefinitonMultiNode.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(identifiersWithGatherNode);
    }
  }

  @Override
  protected void walkPostPrimitiveStatement(final AstNode node) {
    final AstNode numberNode = node.getFirstChild(MagikGrammar.NUMBER);
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(numberNode);
  }

  @Override
  protected void walkPostLoopbody(final AstNode node) {
    final Token lParenToken = AstQuery.getFirstChildToken(node, MagikPunctuator.PAREN_L.getValue());
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(lParenToken);

    final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
    if (tupleNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(tupleNode);
    }

    final Token rParenToken = AstQuery.getFirstChildToken(node, MagikPunctuator.PAREN_R.getValue());
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(rParenToken);
  }
}
