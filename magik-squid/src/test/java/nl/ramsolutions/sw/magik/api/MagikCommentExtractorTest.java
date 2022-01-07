package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MagikCommentExtractor.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class MagikCommentExtractorTest {

    private AstNode parseMagik(String code) {
        final MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testComments() throws IOException {
        final String code = ""
            + "# first comment\n"
            + "print() # comment    \n"
            + "# final comment\n";
        final AstNode node = this.parseMagik(code);
        final List<Token> tokens = MagikCommentExtractor.extractComments(node)
            .collect(Collectors.toList());

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
    void testMethodDoc() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    ## Line 1\n"
            + "    ## Line 2\n"
            + "_endmethod";

        final AstNode node = this.parseMagik(code);
        final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final List<Token> docTokenComments = MagikCommentExtractor.extractDocComments(methodDefNode)
            .collect(Collectors.toList());
        final List<String> docComments = docTokenComments.stream()
            .map(token -> token.getValue())
            .collect(Collectors.toList());

        assertThat(docComments).containsExactly("## Line 1", "## Line 2");
    }

    @Test
    void testMethodDocExtras() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    ## Line 1\n"
            + "    ## Line 2\n"
            + "    # Line 3\n"
            + "_endmethod";

        final AstNode node = this.parseMagik(code);
        final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final List<Token> docTokenComments = MagikCommentExtractor.extractDocComments(methodDefNode)
            .collect(Collectors.toList());
        final List<String> docComments = docTokenComments.stream()
            .map(token -> token.getValue())
            .collect(Collectors.toList());

        assertThat(docComments).containsExactly("## Line 1", "## Line 2");
    }

    @Test
    void testMethodDocPragma() throws IOException {
        final String code = ""
            + "_pragma(a=b)\n"
            + "## Line 1\n"
            + "## Line 2\n"
            + "_method object.test\n"
            + "    ## Line 3\n"
            + "    ## Line 4\n"
            + "_endmethod";

        final AstNode node = this.parseMagik(code);
        final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final List<Token> docTokenComments = MagikCommentExtractor.extractDocComments(methodDefNode)
            .collect(Collectors.toList());
        final List<String> docComments = docTokenComments.stream()
            .map(token -> token.getValue())
            .collect(Collectors.toList());

        assertThat(docComments).containsExactly("## Line 1", "## Line 2", "## Line 3", "## Line 4");
    }

    @Test
    void testMethodDocChildProc() throws IOException {
        final String code = ""
            + "_method object.test\n"
            + "    ## Line 1\n"
            + "    ## Line 2\n"
            + "    _proc()\n"
            + "        ## Line 3\n"
            + "        ## Line 4\n"
            + "    _endproc\n"
            + "_endmethod";

        final AstNode node = this.parseMagik(code);
        final AstNode methodDefNode = node.getFirstChild(MagikGrammar.METHOD_DEFINITION);
        final List<Token> docTokenComments = MagikCommentExtractor.extractDocComments(methodDefNode)
            .collect(Collectors.toList());
        final List<String> docComments = docTokenComments.stream()
            .map(token -> token.getValue())
            .collect(Collectors.toList());

        assertThat(docComments).containsExactly("## Line 1", "## Line 2", "## Line 3", "## Line 4");
    }

    @Test
    void testDocStatement() throws IOException {
        final String code = ""
            + "_pragma(a=b)\n"
            + "## Line 1\n"
            + "## Line 2\n"
            + "object.define_shared_constant(:a, 1, :public)";

        final AstNode node = this.parseMagik(code);
        final AstNode statementNode = node.getFirstChild(MagikGrammar.STATEMENT);
        final List<Token> docTokenComments = MagikCommentExtractor.extractDocComments(statementNode)
            .collect(Collectors.toList());
        final List<String> docComments = docTokenComments.stream()
            .map(token -> token.getValue())
            .collect(Collectors.toList());

        assertThat(docComments).containsExactly("## Line 1", "## Line 2");
    }

}
