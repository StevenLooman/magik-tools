package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/** Standard formatting strategy. */
class MagikFormattingStrategy extends FormattingStrategy {

  private static final List<String> KEYWORDS =
      Collections.unmodifiableList(List.of(MagikKeyword.keywordValues()));

  // We cannot base indenting purely on AstNodes
  // (BODY/PARAMETERS/ARGUMENTS/SIMPLE_VECTOR/...),
  // as bodies and tokens don't play that well together.
  // Tokens surround the AstNodes, e.g.: '(', pre PARAMETERS, post PARAMETERS,
  // ')', or '_method', '...', pre BODY, ..., post BODY, '# comment',
  // '_endmethod'.
  private static final Set<String> INDENT_INCREASE =
      Collections.unmodifiableSet(
          Set.of(
              // MagikPunctuator.PAREN_L.getValue(),
              MagikPunctuator.BRACE_L.getValue(),
              MagikPunctuator.SQUARE_L.getValue(),
              MagikKeyword.PROC.getValue(),
              MagikKeyword.METHOD.getValue(),
              MagikKeyword.BLOCK.getValue(),
              MagikKeyword.TRY.getValue(),
              MagikKeyword.WHEN.getValue(),
              MagikKeyword.PROTECT.getValue(),
              MagikKeyword.PROTECTION.getValue(),
              MagikKeyword.CATCH.getValue(),
              MagikKeyword.LOCK.getValue(),
              MagikKeyword.THEN.getValue(),
              MagikKeyword.ELSE.getValue(),
              MagikKeyword.LOOP.getValue(),
              MagikKeyword.FINALLY.getValue()));

  private static final Set<String> INDENT_DECREASE =
      Collections.unmodifiableSet(
          Set.of(
              // MagikPunctuator.PAREN_R.getValue(),
              MagikPunctuator.BRACE_R.getValue(),
              MagikPunctuator.SQUARE_R.getValue(),
              MagikKeyword.ENDPROC.getValue(),
              MagikKeyword.ENDMETHOD.getValue(),
              MagikKeyword.ENDBLOCK.getValue(),
              MagikKeyword.ENDTRY.getValue(),
              MagikKeyword.WHEN.getValue(),
              MagikKeyword.PROTECTION.getValue(),
              MagikKeyword.ENDPROTECT.getValue(),
              MagikKeyword.ENDCATCH.getValue(),
              MagikKeyword.ENDLOCK.getValue(),
              MagikKeyword.ELSE.getValue(),
              MagikKeyword.ELIF.getValue(),
              MagikKeyword.ENDIF.getValue(),
              MagikKeyword.ENDLOOP.getValue(),
              MagikKeyword.FINALLY.getValue()));

  private static final Set<String> AUGMENTED_ASSIGNMENT_TOKENS =
      Collections.unmodifiableSet(
          Set.of(
              MagikKeyword.IS.getValue(),
              MagikKeyword.ISNT.getValue(),
              MagikKeyword.ANDIF.getValue(),
              MagikKeyword.AND.getValue(),
              MagikKeyword.ORIF.getValue(),
              MagikKeyword.OR.getValue(),
              MagikKeyword.XOR.getValue(),
              MagikKeyword.DIV.getValue(),
              MagikKeyword.MOD.getValue(),
              MagikKeyword.CF.getValue(),
              MagikOperator.PLUS.getValue(),
              MagikOperator.MINUS.getValue(),
              MagikOperator.STAR.getValue(),
              MagikOperator.DIV.getValue(),
              MagikOperator.EXP.getValue(),
              MagikOperator.EQ.getValue(),
              MagikOperator.NEQ.getValue()));

  private int indent;
  private AstNode currentNode;

  MagikFormattingStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  List<TextEdit> walkCommentToken(final Token token) {
    return this.walkToken(token);
  }

  @Override
  List<TextEdit> walkEolToken(final Token token) {
    // Don't touch syntax errors.
    if (this.currentNode.is(MagikGrammar.SYNTAX_ERROR)) {
      return Collections.emptyList();
    }

    // Test distance to lastTextToken, only single empty line allowed.
    final int emptyLineCount =
        this.lastTextToken != null ? token.getLine() - this.lastTextToken.getLine() : 0;
    if (emptyLineCount > 1) {
      // Add edit to remove empty line.
      final TextEdit textEdit = this.editNoNewline(token);
      return List.of(textEdit);
    } else if (this.options.isTrimTrailingWhitespace()
        && this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      final TextEdit textEdit = this.editToken(this.lastToken, "", "no whitespace after allowed");
      return List.of(textEdit);
    }

    return Collections.emptyList();
  }

  @Override
  List<TextEdit> walkToken(final Token token) {
    this.trackIndentPre(token);

    final boolean isFirstTextToken = this.lastTextToken == null;
    final List<TextEdit> textEdits = new ArrayList<>();
    if (isFirstTextToken) {
      // First token, should not contain any pre-whitespace/indenting.
      final TextEdit textEdit = this.editNoWhitespaceBefore(token);
      textEdits.add(textEdit);
    } else {
      final boolean isOnNewline = !token.isOnSameLineThan(this.lastTextToken);
      if (isOnNewline) {
        if (this.requireNewlineBefore(token)) {
          if (this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
            final TextEdit textEdit = this.editNewlineBefore(this.lastToken);
            textEdits.add(textEdit);
          } else {
            final TextEdit textEdit = this.editNewlineBefore(token);
            textEdits.add(textEdit);
          }
        }

        final TextEdit textEdit = this.ensureIndenting(token);
        textEdits.add(textEdit);
      } else {
        final TextEdit textEdit = this.validateWhitespacingBefore(token);
        textEdits.add(textEdit);
      }
    }

    this.trackIndentPost(token);
    return textEdits;
  }

  private TextEdit validateWhitespacingBefore(final Token token) {
    if (this.requireWhitespaceBefore(token)) {
      return this.editWhitespaceBefore(token);
    } else if (this.requireNoWhitespaceBefore(token)) {
      return this.editNoWhitespaceBefore(token);
    }

    return this.editWhitespaceBefore(token);
  }

  private boolean requireNewlineBefore(final Token token) {
    return this.tokenIs(this.lastTextToken, "$")
        && this.lastTextToken.getLine() + 1 == token.getLine();
  }

  private boolean requireWhitespaceBefore(final Token token) {
    final String tokenValue = token.getOriginalValue().toLowerCase();
    final String lastTextTokenValue =
        this.lastTextToken != null ? this.lastTextToken.getOriginalValue().toLowerCase() : null;
    return token.isOnSameLineThan(this.lastTextToken)
        && (KEYWORDS.contains(lastTextTokenValue) // Always whitespace after a keyword.
            || KEYWORDS.contains(tokenValue) // Always whitespace before a keyword.
            || this.tokenIs(token, "<<", "^<<"))
        && !(AUGMENTED_ASSIGNMENT_TOKENS.contains(
                lastTextTokenValue) // But no whitespace before augmented assignment.
            && (this.tokenIs(token, "<<", "^<<")))
        && !this.tokenIs(token, ".", ",", ")", "}", "]")
        && !this.tokenIs(this.lastToken, "(", "{", "[");
  }

  private boolean requireNoWhitespaceBefore(final Token token) {
    final String lastTextTokenValue =
        this.lastTextToken != null ? this.lastTextToken.getOriginalValue().toLowerCase() : null;
    return !this.tokenIs(token, GenericTokenType.COMMENT)
        && (this.tokenIs(token, ")", "}", "]", ",")
            || this.nodeIsSlot()
            || this.tokenIs(this.lastTextToken, "@", "(", "{", "[")
            || this.currentNode.is(MagikGrammar.ARGUMENTS)
            || this.currentNode.is(MagikGrammar.PARAMETERS)
            || this.nodeIsMethodDefinition()
            || this.nodeIsInvocation()
            || this.nodeIsUnaryExpression()
            || AUGMENTED_ASSIGNMENT_TOKENS.contains(lastTextTokenValue)
                && this.tokenIs(token, "<<", "^<<"));
  }

  private boolean nodeIsUnaryExpression() {
    final String lastTokenValue = this.lastTextToken.getOriginalValue();
    final AstNode unaryExprNode = this.currentNode.getFirstAncestor(MagikGrammar.UNARY_EXPRESSION);
    return unaryExprNode != null
        && unaryExprNode.getToken() == this.lastTextToken
        && ("-".equals(lastTokenValue) || "+".equals(lastTokenValue) || "~".equals(lastTokenValue));
  }

  private boolean nodeIsSlot() {
    return this.currentNode.getParent().is(MagikGrammar.SLOT);
  }

  private boolean nodeIsMethodDefinition() {
    return this.currentNode.is(MagikGrammar.METHOD_DEFINITION)
        || this.currentNode
            .getParent()
            .is(
                MagikGrammar.METHOD_DEFINITION,
                MagikGrammar.EXEMPLAR_NAME,
                MagikGrammar.METHOD_NAME);
  }

  private boolean nodeIsInvocation() {
    return this.currentNode.is(MagikGrammar.PROCEDURE_INVOCATION, MagikGrammar.METHOD_INVOCATION)
        || this.currentNode.is(MagikGrammar.IDENTIFIER)
            && this.currentNode.getParent().is(MagikGrammar.METHOD_INVOCATION);
  }

  @Override
  void walkPreNode(final AstNode node) {
    this.currentNode = node;

    if (node.is(MagikGrammar.TRANSMIT)) {
      // Reset indenting.
      this.indent = 0;
    } else if (node.is(
        MagikGrammar.VARIABLE_DEFINITION,
        MagikGrammar.VARIABLE_DEFINITION_MULTI,
        MagikGrammar.PROCEDURE_INVOCATION,
        MagikGrammar.METHOD_INVOCATION)) {
      this.indent += 1;
    }
  }

  @Override
  void walkPostNode(final AstNode node) {
    if (this.isBinaryExpression(node)
        || node.is(
            MagikGrammar.VARIABLE_DEFINITION,
            MagikGrammar.VARIABLE_DEFINITION_MULTI,
            MagikGrammar.PROCEDURE_INVOCATION,
            MagikGrammar.METHOD_INVOCATION)) {
      this.indent -= 1;
    }

    this.currentNode = this.currentNode.getParent();
  }

  private boolean isBinaryExpression(final AstNode node) {
    return node.is(
        MagikGrammar.ASSIGNMENT_EXPRESSION,
        MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION,
        MagikGrammar.OR_EXPRESSION,
        MagikGrammar.XOR_EXPRESSION,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.EQUALITY_EXPRESSION,
        MagikGrammar.RELATIONAL_EXPRESSION,
        MagikGrammar.ADDITIVE_EXPRESSION,
        MagikGrammar.MULTIPLICATIVE_EXPRESSION,
        MagikGrammar.EXPONENTIAL_EXPRESSION);
  }

  @CheckForNull
  private TextEdit ensureIndenting(final Token token) {
    if (this.indent == 0 && !this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      return null;
    }

    final String indentText = this.indentText();
    final String reason = "improper indenting";
    if (!this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      return this.insertBeforeToken(token, indentText, reason);
    } else if (!this.lastToken.getOriginalValue().equals(indentText)) {
      return this.editToken(this.lastToken, indentText, reason);
    }

    return null;
  }

  private String indentText() {
    final int tabSize = this.options.getTabSize();
    final String indentText = this.options.isInsertSpaces() ? " ".repeat(tabSize) : "\t";

    return indentText.repeat(this.indent);
  }

  private void trackIndentPre(final Token token) {
    if (!this.tokenIs(token, GenericTokenType.COMMENT)
        && this.isBinaryExpression(this.currentNode)
        && this.currentNode.getChildren().get(1).getToken() == token) { // Only indent first.
      this.indent += 1;
    }

    final String tokenValue = token.getOriginalValue().toLowerCase();
    if (INDENT_DECREASE.contains(tokenValue)) {
      this.indent -= 1;
    }
  }

  private void trackIndentPost(final Token token) {
    final String tokenValue = token.getOriginalValue().toLowerCase();
    if (INDENT_INCREASE.contains(tokenValue)) {
      this.indent += 1;
    }
  }
}
