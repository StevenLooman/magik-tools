package nl.ramsolutions.sw.magik.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Test MagikCommentExtractor. */
@SuppressWarnings("checkstyle:MagicNumber")
class MagikCommentExtractorTest {

  private AstNode parseMagik(String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testComments() {
    final String code =
        """
        # first comment
        print() # comment\s\s\s\s
        # final comment
        """;
    final AstNode node = this.parseMagik(code);
    final List<Token> tokens = MagikCommentExtractor.extractComments(node).toList();

    assertThat(tokens).hasSize(3);

    final Token token0 = tokens.get(0);
    assertThat(token0.getLine()).isEqualTo(1);
    assertThat(token0.getColumn()).isZero();
    assertThat(token0.getValue()).isEqualTo("# first comment");

    final Token token1 = tokens.get(1);
    assertThat(token1.getLine()).isEqualTo(2);
    assertThat(token1.getColumn()).isEqualTo(8);
    assertThat(token1.getValue()).isEqualTo("# comment    ");

    final Token token2 = tokens.get(2);
    assertThat(token2.getLine()).isEqualTo(3);
    assertThat(token2.getColumn()).isZero();
    assertThat(token2.getValue()).isEqualTo("# final comment");
  }

  @Test
  void testMethodDoc() {
    final String code =
        """
        _method object.test
            ## Line 1
            ## Line 2
        _endmethod""";

    final AstNode node = this.parseMagik(code);
    final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final List<Token> docTokenComments =
        MagikCommentExtractor.extractDocCommentTokens(methodDefNode).toList();
    final List<String> docComments =
        docTokenComments.stream().map(token -> token.getValue()).toList();

    assertThat(docComments).containsExactly("## Line 1", "## Line 2");
  }

  @Test
  void testMethodDocExtras() {
    final String code =
        """
        _method object.test
            ## Line 1
            ## Line 2
            # Line 3
        _endmethod""";

    final AstNode node = this.parseMagik(code);
    final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final List<Token> docTokenComments =
        MagikCommentExtractor.extractDocCommentTokens(methodDefNode).toList();
    final List<String> docComments =
        docTokenComments.stream().map(token -> token.getValue()).toList();

    assertThat(docComments).containsExactly("## Line 1", "## Line 2");
  }

  @Test
  void testMethodDocPragma() {
    final String code =
        """
        _pragma(a=b)
        ## Line 1
        ## Line 2
        _method object.test
            ## Line 3
            ## Line 4
        _endmethod""";

    final AstNode node = this.parseMagik(code);
    final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final List<Token> docTokenComments =
        MagikCommentExtractor.extractDocCommentTokens(methodDefNode).toList();
    final List<String> docComments =
        docTokenComments.stream().map(token -> token.getValue()).toList();

    assertThat(docComments).containsExactly("## Line 1", "## Line 2", "## Line 3", "## Line 4");
  }

  @Test
  void testMethodDocChildProc() {
    final String code =
        """
        _method object.test
            ## Line 1
            ## Line 2
            _proc()
                ## Line 3
                ## Line 4
            _endproc
        _endmethod""";

    final AstNode node = this.parseMagik(code);
    final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
    final List<Token> docTokenComments =
        MagikCommentExtractor.extractDocCommentTokens(methodDefNode).toList();
    final List<String> docComments =
        docTokenComments.stream().map(token -> token.getValue()).toList();

    assertThat(docComments).containsExactly("## Line 1", "## Line 2", "## Line 3", "## Line 4");
  }

  @Test
  void testDocStatement() {
    final String code =
        """
        _pragma(a=b)
        ## Line 1
        ## Line 2
        object.define_shared_constant(:a, 1, :public)""";

    final AstNode node = this.parseMagik(code);
    final AstNode statementNode = node.getFirstChild(MagikGrammar.STATEMENT);
    final List<Token> docTokenComments =
        MagikCommentExtractor.extractDocCommentTokens(statementNode).toList();
    final List<String> docComments =
        docTokenComments.stream().map(token -> token.getValue()).toList();

    assertThat(docComments).containsExactly("## Line 1", "## Line 2");
  }
}
