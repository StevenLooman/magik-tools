package nl.ramsolutions.sw.magik;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

class TextEditGeneratorTest {

  @Test
  void testEqualNoEdits() {
    final String code1 = "a<< 10";
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 = "a<< 10";
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits).isEmpty();
  }

  @Test
  void testWhitespaceAdded() {
    final String code1 = "a<< 10";
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 = "a << 10";
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(1, 1)), " "));
  }

  @Test
  void testWhitespaceRemoved() {
    final String code1 = "a << 10";
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 = "a<< 10";
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(1, 2)), ""));
  }

  @Test
  void testWhitespaceAddedAndRemoved() {
    final String code1 = "a<< 10";
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 = "a <<10";
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 1), new Position(1, 1)), " "),
            new TextEdit(new Range(new Position(1, 3), new Position(1, 4)), ""));
  }

  @Test
  void testLineCommentAdded() {
    final String code1 =
        """
        a << 10
        """;
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 =
        """
        # comment
        a << 10
        """;
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "# comment\n"));
  }

  @Test
  void testLineCommentRemoved() {
    final String code1 =
        """
        # comment
        a << 10
        """;
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 =
        """
        a << 10
        """;
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 0), new Position(2, 0)), ""));
  }
}
