package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MagikParser}. */
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
    final String code =
        """
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
    final String code =
        """
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
    assertThat(eofToken.getColumn()).isZero();
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
    final String code =
        """
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
    final String code =
        """
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
    final String code =
        """
        _a
        $
        a()
        $
        """;
    final AstNode node = this.parseMagik(code);

    assertThat(node.getChildren()).hasSize(5);
  }

  @Test
  void testMultipleAssignmentGatherTargetIsAnAssignable() {
    final String code = "(a, _gather b) << (1, 2)";
    final AstNode node = this.parseMagik(code);

    final AstNode assignablesNode =
        node.getFirstDescendant(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
    final List<AstNode> expressionNodes = assignablesNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
    final AstNode gatherTargetNode = expressionNodes.get(1);
    final AstNode gatherTokenNode = gatherTargetNode.getPreviousSibling();
    final String gatherTokenValue = gatherTokenNode.getTokenValue();
    assertThat(gatherTokenValue).isEqualTo("_gather");
    final AstNode gatherExpressionNode =
        assignablesNode.getFirstDescendant(MagikGrammar.GATHER_EXPRESSION);
    assertThat(gatherExpressionNode).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return 7 _scatter v",
        "_return 7\n_scatter v",
        "_return 7\n\n_scatter v",
        "_return 7 # note\n_scatter v",
        ">> 7 _scatter v",
        ">> 7\n_scatter v",
        "_leave _with 7 _scatter v",
        "_leave _with 7\n_scatter v",
        "_continue _with 7 _scatter v",
        "_continue _with 7\n_scatter v",
        "_throw :t _with 7 _scatter v",
        "_throw :t _with 7\n_scatter v",
        "_loopbody(7 _scatter v)",
        "_loopbody(7\n_scatter v)",
        "_return (7 _scatter v)",
        "_return (7\n_scatter v)",
        ">> (7 _scatter v)",
        "_leave _with (7 _scatter v)",
        "_continue _with (7 _scatter v)",
        "_throw :t _with (7 _scatter v)",
        "(a, b, c) << (7 _scatter v)",
        "(_gather r) << (7 _scatter v)",
        "_local (a, b, c) << (7 _scatter v)",
        "_local (_gather r) << (7 _scatter v)"
      })
  void testScatterWithoutACommaIsTheLastExpressionOfOneTuple(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final List<AstNode> statementNodes = node.getChildren(MagikGrammar.STATEMENT);
    assertThat(statementNodes).hasSize(1);
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"_return 1, 7 _scatter v", "_return (1, 7 _scatter v)"})
  void testScatterWithoutACommaFollowsEveryExpressionOfATuple(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(3);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return (1, 2) _scatter v",
        "_return (1) _scatter v",
        "_return (7 _scatter v) _scatter v"
      })
  void testParenthesesBeforeAScatterAreAValue(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final AstNode parenthesisNode = tupleNode.getFirstChild();
    final AstNodeType parenthesisType = parenthesisNode.getType();
    assertThat(parenthesisType).isEqualTo(MagikGrammar.EXPRESSION);
    final AstNode atomNode = parenthesisNode.getFirstChild();
    final AstNodeType atomType = atomNode.getType();
    assertThat(atomType).isEqualTo(MagikGrammar.ATOM);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @Test
  void testParenthesisedScatterAsAValueIsAnAtom() {
    final String code = "p((7 _scatter v), 1)";
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode argumentNode = node.getFirstDescendant(MagikGrammar.ARGUMENT);
    final AstNode expressionNode = argumentNode.getFirstChild(MagikGrammar.EXPRESSION);
    final AstNode atomNode = expressionNode.getFirstChild();
    final AstNodeType atomType = atomNode.getType();
    assertThat(atomType).isEqualTo(MagikGrammar.ATOM);
    final List<AstNode> expressionNodes = atomNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return 7\n, 8",
        "_return 7\n\n, 8",
        "_return 7 # note\n, 8",
        ">> 7\n, 8",
        "_leave _with 7\n, 8",
        "_continue _with 7\n, 8",
        "_throw :t _with 7\n, 8",
        "_loopbody(7\n, 8)"
      })
  void testCommaOnTheNextLineStaysInOneTuple(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final List<AstNode> statementNodes = node.getChildren(MagikGrammar.STATEMENT);
    assertThat(statementNodes).hasSize(1);
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return (7, 8)",
        "_return (7, 8); _local x",
        "_if v _then _return (7, 8) _endif",
        "_loop _leave _with (7, 8) _endloop",
        "_return (7, 8) # note",
        "_block >> (7, 8) _endblock"
      })
  void testParenthesisedTupleKeepsItsExpressions(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "(a, b) << (1, 2)\n(c, d) << (3, 4)",
        "(a, b) << (1, 2)\n.x << a",
        "(a, b) << (1, 2) # note\n.x << a",
        "_local (a, b) << (1, 2)\n(c, d) << (3, 4)",
        "_local (a, b) << (1, 2)\n.x << a",
        ">> (7, 8)\n.class_name",
        ">> (7, 8)\n+ 1",
        ">> (7, 8)\n- 1",
        ">> (7, 8)\n_is 7",
        ">> (_true, 8)\n_and _true",
        ">> ({7}, 8)\n[1]",
        ">> (p, 8)\n()"
      })
  void testLineBreakEndsAParenthesisedTupleBeforeASendOrOperator(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return (1, 2).x",
        ">> (1, 2).x",
        ">> (1, 2)[1]",
        ">> (1, 2)()",
        ">> (1, 2) + 1",
        ">> (1, 2) < 1",
        "(a, b) << (1, 2).x",
        "_return (7 _scatter v).x"
      })
  void testParenthesesBeforeAContinuationAreOneValue(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(1);
    final AstNode atomNode = tupleNode.getFirstDescendant(MagikGrammar.ATOM);
    final List<AstNode> atomExpressionNodes = atomNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(atomExpressionNodes).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_return (1, 2), 3",
        ">> (1, 2), 3",
        ">> (1, 2)\n, 3",
        "_leave _with (1, 2), 3",
        "_loopbody((1, 2), 3)"
      })
  void testParenthesesBeforeACommaAreAValue(final String code) {
    final AstNode node = this.parseMagik(code);

    final AstNode syntaxErrorNode = node.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNode).isNull();
    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(2);
    final AstNode firstExpressionNode = expressionNodes.get(0);
    final AstNode atomNode = firstExpressionNode.getFirstChild();
    final AstNodeType atomType = atomNode.getType();
    assertThat(atomType).isEqualTo(MagikGrammar.ATOM);
    final List<AstNode> atomExpressionNodes = atomNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(atomExpressionNodes).hasSize(2);
  }

  @Test
  void testParenthesisedValueBeforeASendStaysAnAtom() {
    final String code = "_return (7).x";
    final AstNode node = this.parseMagik(code);

    final AstNode tupleNode = node.getFirstDescendant(MagikGrammar.TUPLE);
    final AstNode parenthesisNode = tupleNode.getFirstChild();
    final AstNodeType parenthesisType = parenthesisNode.getType();
    assertThat(parenthesisType).isEqualTo(MagikGrammar.EXPRESSION);
    final List<AstNode> expressionNodes = tupleNode.getChildren(MagikGrammar.EXPRESSION);
    assertThat(expressionNodes).hasSize(1);
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
