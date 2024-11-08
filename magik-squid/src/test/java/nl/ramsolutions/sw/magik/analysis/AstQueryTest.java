package nl.ramsolutions.sw.magik.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Test AstQuery. */
@SuppressWarnings("checkstyle:MagicNumber")
class AstQueryTest {

  private AstNode parseCode(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testNodeBefore1() {
    final String code =
        """
        _method a.b
          1
        _endmethod
        """;
    final AstNode node = this.parseCode(code);
    final Position position = new Position(2, 3);
    final AstNode nodeBefore = AstQuery.nodeBefore(node, position);
    assertThat(nodeBefore).isNotNull();
    assertThat(nodeBefore.getTokenValue()).isEqualTo("1");
  }

  @Test
  void testNodeBefore2() {
    final String code =
        """
        _method a.b
        _endmethod
        """;
    final AstNode node = this.parseCode(code);
    final Position position = new Position(2, 0);
    final AstNode nodeBefore = AstQuery.nodeBefore(node, position);
    assertThat(nodeBefore).isNotNull();
    assertThat(nodeBefore.getTokenValue()).isEqualTo("b");
  }

  @Test
  void testNodeAfter1() {
    final String code =
        """
        _method a.b
          1
        _endmethod
        """;
    final AstNode node = this.parseCode(code);
    final Position position = new Position(1, 11);
    final AstNode nodeAfter = AstQuery.nodeAfter(node, position);
    assertThat(nodeAfter).isNotNull();
    assertThat(nodeAfter.getTokenValue()).isEqualTo("1");
  }

  @Test
  void testNodeAfter2() {
    final String code =
        """
        _method a.b
        _endmethod
        """;
    final AstNode node = this.parseCode(code);
    final Position position = new Position(2, 0);
    final AstNode nodeAfter = AstQuery.nodeAfter(node, position);
    assertThat(nodeAfter).isNotNull();
    assertThat(nodeAfter.getTokenValue()).isEqualTo("_endmethod");
  }

  @Test
  void testNodeAtFound() {
    final String code =
        """
        a << 10
        b << 20""";
    final AstNode node = this.parseCode(code);
    final Position startPosition = new Position(1, 2);
    final Position endPosition = new Position(1, 3);
    final AstNode nodeAtStart = AstQuery.nodeAt(node, startPosition);
    assertThat(nodeAtStart).isNotNull();

    final Token tokenAtStart = nodeAtStart.getToken();
    assertThat(tokenAtStart.getLine()).isEqualTo(1);
    assertThat(tokenAtStart.getColumn()).isEqualTo(2);
    assertThat(tokenAtStart.getOriginalValue()).isEqualTo("<<");

    final AstNode nodeAtEnd = AstQuery.nodeAt(node, endPosition);
    assertThat(nodeAtEnd).isNotNull();

    final Token tokenAtEnd = nodeAtStart.getToken();
    assertThat(tokenAtEnd.getLine()).isEqualTo(1);
    assertThat(tokenAtEnd.getColumn()).isEqualTo(2);
    assertThat(tokenAtEnd.getOriginalValue()).isEqualTo("<<");
  }

  @Test
  void testNodeAtNotFound1() {
    final String code =
        """
        a << 10
        b << 20""";
    final AstNode node = this.parseCode(code);
    final AstNode nodeAt = AstQuery.nodeAt(node, new Position(1, 4));

    assertThat(nodeAt).isNull();
  }

  @Test
  void testNodeAtNotFound2() {
    final String code =
        """
        a << 10
        b << 20""";
    final AstNode node = this.parseCode(code);
    final AstNode nodeAt = AstQuery.nodeAt(node, new Position(3, 4));

    assertThat(nodeAt).isNull();
  }
}
