package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test AstQuery.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class AstQueryTest {

    private AstNode parseCode(String code) {
        MagikParser parser = new MagikParser();
        return parser.parseSafe(code);
    }

    @Test
    void testNodeAtFound() {
        String code = ""
                + "a << 10\n"
                + "b << 20";
        AstNode node = this.parseCode(code);
        AstNode nodeAt = AstQuery.nodeAt(node, new Position(1, 4));
        assertThat(nodeAt).isNotNull();

        Token tokenAt = nodeAt.getToken();
        assertThat(tokenAt.getLine()).isEqualTo(1);
        assertThat(tokenAt.getColumn()).isEqualTo(2);
        assertThat(tokenAt.getOriginalValue()).isEqualTo("<<");
    }

    @Test
    void testNodeAtNotFound() {
        String code = ""
                + "a << 10\n"
                + "b << 20";
        AstNode node = this.parseCode(code);
        AstNode nodeAt = AstQuery.nodeAt(node, new Position(3, 4));

        assertThat(nodeAt).isNull();
    }

}
