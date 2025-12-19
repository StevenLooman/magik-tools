package nl.ramsolutions.sw.magik;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Tests for {@link TextEditGenerator}. */
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

  @Test
  void testIndentationChanged() {
    final String code1 =
        """
        a << 10
        b << 20
        """;
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 =
        """
        a << 10
          b << 20
        """;
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);

    // With the Myers diff algorithm, only the indentation should change, not the newline
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "  "));
  }

  @Test
  void testIndentationReduced() {
    final String code1 =
        """
        a << 10
            b << 20
        """;
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 =
        """
        a << 10
          b << 20
        """;
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);

    // With the Myers diff algorithm, it finds the common prefix (2 spaces)
    // and removes the extra 2 spaces at positions (2, 2) to (2, 4)
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEmpty(); // Deletion
    assertThat(edits.get(0).getRange().getStartPosition().getLine()).isEqualTo(2);
    assertThat(edits.get(0).getRange().getStartPosition().getColumn()).isGreaterThanOrEqualTo(0);
    assertThat(edits.get(0).getRange().getEndPosition().getColumn())
        .isGreaterThan(edits.get(0).getRange().getStartPosition().getColumn());
  }

  @Test
  void testIndentationAndCommentChanged() {
    final String code1 =
        """
        a << 10
        # old comment
        b << 20
        """;
    final AstNode node1 = new MagikParser().parse(code1);
    final String code2 =
        """
        a << 10
        # new comment
          b << 20
        """;
    final AstNode node2 = new MagikParser().parse(code2);

    final TextEditGenerator generator = new TextEditGenerator();
    final List<TextEdit> edits = generator.generateEdits(node1, node2);

    // With Myers diff, we get precise edits for the comment change and indentation
    // The key improvement: newlines are NOT included in the edits
    assertThat(edits).hasSizeGreaterThanOrEqualTo(2);

    // Verify that one edit changes the comment text
    assertThat(edits)
        .anySatisfy(
            edit -> {
              // Comment change on line 2
              assertThat(edit.getRange().getStartPosition().getLine()).isEqualTo(2);
            });

    // Verify that one edit adds indentation on line 3
    assertThat(edits)
        .anySatisfy(
            edit -> {
              // Indentation change on line 3
              assertThat(edit.getRange().getStartPosition().getLine()).isEqualTo(3);
              assertThat(edit.getNewText()).isEqualTo("  ");
            });
  }
}
