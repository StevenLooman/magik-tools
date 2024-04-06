package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Test MagikParser. */
@SuppressWarnings("checkstyle:MagicNumber")
class MagikParserTest {

  private AstNode parseMagik(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testParseIdentifier() {
    assertThat(MagikParser.parseIdentifier("abc")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("ABC")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("abc|def|")).isEqualTo("abcdef");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("abc|def|ghi")).isEqualTo("abcdefghi");
    assertThat(MagikParser.parseIdentifier("abc|DEF|")).isEqualTo("abcDEF");
    assertThat(MagikParser.parseIdentifier("abc|DEF|ghi|JKL|")).isEqualTo("abcDEFghiJKL");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("\\|")).isEqualTo("|");
    assertThat(MagikParser.parseIdentifier("\\|a")).isEqualTo("|a");
  }

  @Test
  void testValidMagik1() {
    final String code = """
        _block
        	write(1)
        _endblock""";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode blockNode = node.getChildren().get(0);
    assertThat(blockNode.getType()).isEqualTo(MagikGrammar.STATEMENT);
    final Token blockToken = blockNode.getToken();
    assertThat(blockToken.getLine()).isEqualTo(1);
    assertThat(blockToken.getColumn()).isZero();

    final AstNode eofNode = node.getChildren().get(1);
    assertThat(eofNode.getType()).isEqualTo(GenericTokenType.EOF);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isEqualTo(9);
  }

  @Test
  void testInvalidMagik1() {
    final String code = """
        _block
        _endbloc
        $
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(3);

    final AstNode syntaxErrorNode = node.getChildren().get(0);
    assertThat(syntaxErrorNode.getType()).isEqualTo(MagikGrammar.SYNTAX_ERROR);
    final Token syntaxErrorToken = syntaxErrorNode.getToken();
    assertThat(syntaxErrorToken.getLine()).isEqualTo(1);
    assertThat(syntaxErrorToken.getColumn()).isZero();

    final AstNode transmitNode = node.getChildren().get(1);
    final Token transmitToken = transmitNode.getToken();
    assertThat(transmitToken.getLine()).isEqualTo(3);
    assertThat(transmitToken.getColumn()).isZero();

    final AstNode eofNode = node.getChildren().get(2);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(4);
    assertThat(eofToken.getColumn()).isZero();
  }

  @Test
  void testInvalidMagik2() {
    final String code =
        """
        _method object.m1
        	_block
        	_endbloc
        _endmethod""";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode methodDefinitionNode = node.getChildren().get(0);
    assertThat(methodDefinitionNode.getType()).isEqualTo(MagikGrammar.METHOD_DEFINITION);
    assertThat(methodDefinitionNode.getToken().getLine()).isEqualTo(1);
    assertThat(methodDefinitionNode.getToken().getColumn()).isZero();

    final AstNode syntaxErrorNode = node.getChildren().get(0).getChildren().get(2);
    assertThat(syntaxErrorNode.getType()).isEqualTo(MagikGrammar.SYNTAX_ERROR);

    final AstNode eofNode = node.getChildren().get(1);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(4);
    assertThat(eofToken.getColumn()).isEqualTo(10);
  }

  @Test
  void testInvalidMagik3() {
    final String code =
        """
        _method object.m2
        	_local a << {1 2}
        _endmethod""";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode methodDefinitionNode = node.getChildren().get(0);
    assertThat(methodDefinitionNode.getType()).isEqualTo(MagikGrammar.METHOD_DEFINITION);
    assertThat(methodDefinitionNode.getToken().getLine()).isEqualTo(1);
    assertThat(methodDefinitionNode.getToken().getColumn()).isZero();

    final AstNode simpleVectorNode =
        methodDefinitionNode.getFirstDescendant(MagikGrammar.SIMPLE_VECTOR);
    assertThat(simpleVectorNode).isNotNull();
    final AstNode syntaxErrorNode = simpleVectorNode.getChildren().get(1);
    assertThat(syntaxErrorNode.getType()).isEqualTo(MagikGrammar.SYNTAX_ERROR);

    final AstNode eofNode = node.getChildren().get(1);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isEqualTo(10);
  }

  @Test
  void testInvalidMagik4() {
    final String code =
        """
        _method object.m3
        	_local a << prc(:)
        _endmethod""";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode methodDefinitionNode = node.getChildren().get(0);
    assertThat(methodDefinitionNode.getType()).isEqualTo(MagikGrammar.METHOD_DEFINITION);
    assertThat(methodDefinitionNode.getToken().getLine()).isEqualTo(1);
    assertThat(methodDefinitionNode.getToken().getColumn()).isZero();

    final AstNode argumentsNode = methodDefinitionNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
    final AstNode syntaxErrorNode = argumentsNode.getChildren().get(1);
    assertThat(syntaxErrorNode.getType()).isEqualTo(MagikGrammar.SYNTAX_ERROR);

    final AstNode eofNode = node.getChildren().get(1);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isEqualTo(10);
  }

  @Test
  void testInvalidMagik5() {
    final String code =
        """
        _pragma(classify_level=restricted)
        _method a.b _block _endmethod
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(3);

    final AstNode pragmaNode = node.getChildren().get(0);
    assertThat(pragmaNode.getType()).isEqualTo(MagikGrammar.PRAGMA);
    assertThat(pragmaNode.getToken().getLine()).isEqualTo(1);
    assertThat(pragmaNode.getToken().getColumn()).isZero();

    final AstNode methodDefinitionNode = node.getChildren().get(1);
    assertThat(methodDefinitionNode.getType()).isEqualTo(MagikGrammar.METHOD_DEFINITION);
    assertThat(methodDefinitionNode.getToken().getLine()).isEqualTo(2);
    assertThat(methodDefinitionNode.getToken().getColumn()).isZero();

    final AstNode eofNode = node.getChildren().get(2);
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isEqualTo(0);
  }

  @Test
  void testInvalidMagik6() {
    final String code = "_local (a, b)";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode syntaxErrorNode = node.getChildren().get(0);
    assertThat(syntaxErrorNode.getType()).isEqualTo(MagikGrammar.SYNTAX_ERROR);
    final Token syntaxErrorToken = syntaxErrorNode.getToken();
    assertThat(syntaxErrorToken.getLine()).isEqualTo(1);
  }

  @Test
  void testCommentBefore() {
    final String code = """
            # comment 1
            # comment 2
        $""";
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode transmitNode = node.getFirstChild();
    final Token transmitToken = transmitNode.getToken();
    assertThat(transmitToken.getLine()).isEqualTo(3);
    assertThat(transmitToken.getColumn()).isZero();
    assertThat(transmitToken.getTrivia()).hasSize(6);

    final AstNode eofNode = node.getLastChild();
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isEqualTo(1);
  }

  @Test
  void testCommentAfter() {
    final String code = """
        $
        # comment 1
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode transmitNode = node.getFirstChild();
    final Token transmitToken = transmitNode.getToken();
    assertThat(transmitToken.getLine()).isEqualTo(1);
    assertThat(transmitToken.getColumn()).isZero();
    assertThat(transmitToken.getTrivia()).isEmpty();

    final AstNode eofNode = node.getLastChild();
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(3);
    assertThat(eofToken.getColumn()).isZero();
    assertThat(eofToken.getTrivia()).hasSize(3);
  }

  @Test
  void testSyntaxError() {
    final String code =
        """
        _block
          _a xxx
            _b aaa bbb
        _endblock
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(2);

    final AstNode blockNode = node.getFirstDescendant(MagikGrammar.BLOCK);
    final AstNode syntaxErrorNode = blockNode.getChildren(MagikGrammar.SYNTAX_ERROR).get(0);
    assertThat(syntaxErrorNode.getChildren()).hasSize(1);
    final AstNode tokenNode0 = syntaxErrorNode.getChildren().get(0);
    final Token tokenNode0token = tokenNode0.getToken();
    assertThat(tokenNode0token.getLine()).isEqualTo(2);
    assertThat(tokenNode0token.getColumn()).isEqualTo(2);
    assertThat(tokenNode0token.getOriginalValue()).isEqualTo("_a xxx\n    _b aaa bbb\n");

    final AstNode eofNode = node.getLastChild();
    final Token eofToken = eofNode.getToken();
    assertThat(eofToken.getLine()).isEqualTo(5);
    assertThat(eofToken.getColumn()).isZero();
  }

  @Test
  void testSyntaxError2() {
    final String code = """
        _a
        $
        a()
        $
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(5);
  }

  @Test
  void testWhitespaceTrivia() {
    final String code =
        """
        _block
          # comment 1
          # comment 2
        _endblock
        """;
    final AstNode node = this.parseMagik(code);
    final AstNode blockNode = node.getFirstDescendant(MagikGrammar.BLOCK);
    final AstNode endblockTokenNode = blockNode.getLastChild();
    assertThat(endblockTokenNode.getTokenOriginalValue()).isEqualTo("_endblock");

    final List<Trivia> trivia = endblockTokenNode.getToken().getTrivia();
    assertThat(trivia).hasSize(7);

    final Trivia trivia0 = trivia.get(0);
    assertThat(trivia0.getToken().getType()).isEqualTo(GenericTokenType.EOL);
    assertThat(trivia0.getToken().getOriginalValue()).isEqualTo("\n");

    final Trivia trivia1 = trivia.get(1);
    assertThat(trivia1.getToken().getType()).isEqualTo(GenericTokenType.WHITESPACE);
    assertThat(trivia1.getToken().getOriginalValue()).isEqualTo("  ");

    final Trivia trivia2 = trivia.get(2);
    assertThat(trivia2.getToken().getType()).isEqualTo(GenericTokenType.COMMENT);
    assertThat(trivia2.getToken().getOriginalValue()).isEqualTo("# comment 1");

    final Trivia trivia3 = trivia.get(3);
    assertThat(trivia3.getToken().getType()).isEqualTo(GenericTokenType.EOL);
    assertThat(trivia3.getToken().getOriginalValue()).isEqualTo("\n");

    final Trivia trivia4 = trivia.get(4);
    assertThat(trivia4.getToken().getType()).isEqualTo(GenericTokenType.WHITESPACE);
    assertThat(trivia4.getToken().getOriginalValue()).isEqualTo("  ");

    final Trivia trivia5 = trivia.get(5);
    assertThat(trivia5.getToken().getType()).isEqualTo(GenericTokenType.COMMENT);
    assertThat(trivia5.getToken().getOriginalValue()).isEqualTo("# comment 2");

    final Trivia trivia6 = trivia.get(6);
    assertThat(trivia6.getToken().getType()).isEqualTo(GenericTokenType.EOL);
    assertThat(trivia6.getToken().getOriginalValue()).isEqualTo("\n");
  }
}
