package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Tests for {@link TokenTriviaEditor}. */
class TokenTriviaEditorTest {

  private static final TokenType IGNORED_TOKEN_TYPE =
      new TokenType() {
        @Override
        public String getName() {
          return "IGNORED";
        }

        @Override
        public String getValue() {
          return this.getName();
        }

        @Override
        public boolean hasToBeSkippedFromAst(AstNode node) {
          return false;
        }
      };

  private static String stringifyTokens(final AstNode node) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final Token token : node.getTokens()) {
      for (final Trivia trivia : token.getTrivia()) {
        for (final Token triviaToken : trivia.getTokens()) {
          final String triviaTokenValue = triviaToken.getOriginalValue();
          stringBuilder.append(triviaTokenValue);
        }
      }

      final String originalValue = token.getOriginalValue();
      stringBuilder.append(originalValue);
    }

    return stringBuilder.toString();
  }

  /**
   * Assert {@link Token}s and {@link Trivia} are equal.
   *
   * @param actualToken {@link Token} A to compare.
   * @param expectedToken {@link Token} B to compare.
   */
  private void assertTokenEquals(final Token actualToken, final Token expectedToken) {
    assertThat(actualToken.getOriginalValue())
        .describedAs("Original value")
        .isEqualTo(expectedToken.getOriginalValue());
    assertThat(actualToken.getValue()).describedAs("Value").isEqualTo(expectedToken.getValue());
    assertThat(actualToken.getLine()).describedAs("Line").isEqualTo(expectedToken.getLine());
    assertThat(actualToken.getColumn()).describedAs("Column").isEqualTo(expectedToken.getColumn());

    if (expectedToken.getType() != IGNORED_TOKEN_TYPE) {
      assertThat(actualToken.getType()).describedAs("Type").isEqualTo(expectedToken.getType());
    }

    assertThat(actualToken.getTrivia().size())
        .describedAs("Trivia count")
        .isEqualTo(expectedToken.getTrivia().size());
    for (int i = 0; i < actualToken.getTrivia().size(); ++i) {
      final Trivia triviaActual = actualToken.getTrivia().get(i);
      final Trivia triviaExpected = expectedToken.getTrivia().get(i);

      this.assertTriviaEquals(triviaActual, triviaExpected);
    }
  }

  private void assertTriviaEquals(final Trivia triviaActual, final Trivia triviaExpected) {
    final List<Token> triviaTokensActual = triviaActual.getTokens();
    final List<Token> triviaTokensExpected = triviaExpected.getTokens();
    assertThat(triviaTokensActual.size()).isEqualTo(triviaTokensExpected.size());

    for (int j = 0; j < triviaTokensActual.size(); ++j) {
      final Token triviaTokenActual = triviaTokensActual.get(j);
      final Token triviaTokenExpected = triviaTokensExpected.get(j);
      this.assertTokenEquals(triviaTokenActual, triviaTokenExpected);
    }
  }

  @Test
  void testInsertWhitespaceBeforeToken() {
    final String code = "a<< 10";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(1); // << token.
    editor.addWhitespaceBefore(token, " ");

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("a << 10");

    this.assertTokenEquals(
        node.getTokens().get(0), // `a` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("a")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `<<` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(2)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("<<")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(1)
                            .setType(GenericTokenType.WHITESPACE)
                            .setValueAndOriginalValue(" ")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(4)
                            .setType(GenericTokenType.WHITESPACE)
                            .setValueAndOriginalValue(" ")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void testRemoveWhitespaceBeforeToken() {
    final String code = "a << 10";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(1); // << token.
    final Token whitespaceToken = token.getTrivia().get(0).getToken();
    editor.removeWhitespaceToken(whitespaceToken);

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("a<< 10");

    this.assertTokenEquals(
        node.getTokens().get(0), // `a` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("a")
            .setTrivia(List.of()) // No Trivia.
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `<<` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(1)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("<<")
            .setTrivia(List.of()) // No Trivia.
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(3)
                            .setType(GenericTokenType.WHITESPACE)
                            .setValueAndOriginalValue(" ")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(6)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void testRemoveAndAddWhitespaceBeforeToken() {
    final String code = "a  << 10";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(1); // << token.
    final Token whitespaceToken = token.getTrivia().get(0).getToken();
    editor.removeWhitespaceToken(whitespaceToken);
    editor.addWhitespaceBefore(token, " ");

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("a << 10");

    this.assertTokenEquals(
        node.getTokens().get(0), // `a` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("a")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `<<` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(2)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("<<")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(1)
                            .setType(GenericTokenType.WHITESPACE)
                            .setValueAndOriginalValue(" ")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(4)
                            .setType(GenericTokenType.WHITESPACE)
                            .setValueAndOriginalValue(" ")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void testAddEolBeforeToken1() {
    final String code = "show(10)";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(0); // show token.
    editor.addEolBefore(token, "\n");

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("\nshow(10)");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(0)
                            .setType(GenericTokenType.EOL)
                            .setValueAndOriginalValue("\n")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(8)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void testAddEolBeforeToken2() {
    final String code = "# comment\nshow(10)";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(0); // show token.
    editor.addEolBefore(token, "\n");

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("# comment\n\nshow(10)");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(3)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(0)
                            .setType(GenericTokenType.COMMENT)
                            .setValueAndOriginalValue("# comment")
                            .build()),
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(9)
                            .setType(GenericTokenType.EOL)
                            .setValueAndOriginalValue("\n")
                            .build()),
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(2)
                            .setColumn(0)
                            .setType(GenericTokenType.EOL)
                            .setValueAndOriginalValue("\n")
                            .build())))
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(3)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(3)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(3)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(3)
            .setColumn(8)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void testAddEolBeforeToken3() {
    final String code = "show(10)";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(4); // EOF token.
    editor.addEolBefore(token, "\n");

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("show(10)\n");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(2)
            .setColumn(0)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .setTrivia(
                List.of(
                    Trivia.createSkippedText(
                        Token.builder()
                            .setURI(URI.create("memory:///source.magik"))
                            .setLine(1)
                            .setColumn(8)
                            .setType(GenericTokenType.EOL)
                            .setValueAndOriginalValue("\n")
                            .build())))
            .build());
  }

  @Test
  void removeEolBeforeToken1() {
    final String code = "\nshow(10)";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(0); // `show` token.
    final Token triviaToken = token.getTrivia().get(0).getToken(); // EOL trivia.
    editor.removeEolToken(triviaToken);

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("show(10)");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(8)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void removeEolBeforeToken2() {
    final String code = "show(\n10)";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(2); // `10` token.
    final Token triviaToken = token.getTrivia().get(0).getToken(); // EOL trivia.
    editor.removeEolToken(triviaToken);

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("show(10)");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(8)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }

  @Test
  void removeEolBeforeToken3() {
    final String code = "show(10)\n";
    final AstNode node = new MagikParser().parse(code);
    final TokenTriviaEditor editor = new TokenTriviaEditor(node);
    final Token token = node.getTokens().get(4); // `EOF` token.
    final Token triviaToken = token.getTrivia().get(0).getToken(); // EOL trivia.
    editor.removeEolToken(triviaToken);

    final String actualCode = TokenTriviaEditorTest.stringifyTokens(node);
    assertThat(actualCode).isEqualTo("show(10)");

    this.assertTokenEquals(
        node.getTokens().get(0), // `show` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(0)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("show")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(1), // `(` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(4)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("(")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(2), // `10` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(5)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue("10")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(3), // `)` token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(7)
            .setType(IGNORED_TOKEN_TYPE)
            .setValueAndOriginalValue(")")
            .build());
    this.assertTokenEquals(
        node.getTokens().get(4), // EOF token.
        Token.builder()
            .setURI(URI.create("memory:///source.magik"))
            .setLine(1)
            .setColumn(8)
            .setType(GenericTokenType.EOF)
            .setValueAndOriginalValue("")
            .build());
  }
}
