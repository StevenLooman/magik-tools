package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/** Standard formatting strategy. */
class MagikFormattingStrategy extends AbstractFormattingStrategy {

  private static final List<String> KEYWORDS =
      Collections.unmodifiableList(List.of(MagikKeyword.keywordValues()));

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

  private final IndentStrategy indentStrategy;
  private AstNode currentNode;

  MagikFormattingStrategy(final FormattingOptions options) {
    super(options);
    this.indentStrategy = new TabbedIndentStrategy(options);
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
    final List<TextEdit> textEdits = new ArrayList<>();
    if (this.lastTextToken == null) {
      // First token, should not contain any pre-whitespace/indenting.
      final TextEdit textEdit = this.editNoWhitespaceBefore(token);
      textEdits.add(textEdit);
    } else {
      if (!token.isOnSameLineThan(this.lastTextToken)) {
        if (this.requireNewlineBefore(token)) {
          if (this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
            final TextEdit textEdit = this.editNewlineBefore(this.lastToken);
            textEdits.add(textEdit);
          } else {
            final TextEdit textEdit = this.editNewlineBefore(token);
            textEdits.add(textEdit);
          }
        }

        final TextEdit textEdit = this.indentStrategy.ensureIndenting(token, this.currentNode);
        textEdits.add(textEdit);
      } else {
        final TextEdit textEdit = this.validateWhitespacingBefore(token);
        textEdits.add(textEdit);
      }
    }

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
            && !this.tokenIs(this.lastToken, "(", "{", "[")
            && !this.tokenIs(
                this.lastTextToken,
                // Parameters of a nameless procedure definition.
                "_proc",
                // All end keywords, followed by `(`, `[`.
                "_endblock",
                "_endcatch",
                "_endif",
                "_endlock",
                "_endloop",
                "_endproc",
                "_endprotect",
                "_endtry",
                // Self, super, private.
                // TODO: Is invalid `_private()`, but `_private[]` is valid.
                "_self",
                "_super",
                "_private",
                // Loopbody.
                "_loopbody")
        || (this.tokenIs(this.lastTextToken, "_private", "_iter", "_abstract")
            && this.tokenIs(token, "_method"));
  }

  private boolean requireNoWhitespaceBefore(final Token token) {
    final String lastTextTokenValue =
        this.lastTextToken != null ? this.lastTextToken.getOriginalValue().toLowerCase() : null;
    return !this.tokenIs(token, GenericTokenType.COMMENT)
        && (this.tokenIs(token, ")", "}", "]", ",")
            || this.tokenIs(this.lastTextToken, "@", "(", "{", "[", "_proc", "_loopbody", "_super")
            || this.nodeIsSlot()
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
  }

  @Override
  void walkPostNode(final AstNode node) {
    this.currentNode = this.currentNode.getParent();
  }

  @Override
  void setLastToken(final Token token) {
    this.indentStrategy.setLastToken(token);
    super.setLastToken(token);
  }
}
