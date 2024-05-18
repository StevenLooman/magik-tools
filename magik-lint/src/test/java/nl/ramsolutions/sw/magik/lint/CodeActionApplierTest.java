package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;

/** Tests for {@link CodeActionApplier}. */
class CodeActionApplierTest {

  @Test
  void testApplyCodeAction() {
    final String source = "Hello, world!\n";
    final CodeActionApplier codeActionApplier = new CodeActionApplier(source);
    codeActionApplier.apply(
        new CodeAction(
            "test fix",
            new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "New line!\n")));
    final String newSource = codeActionApplier.getSource();
    assertThat(newSource).isEqualTo("New line!\nHello, world!\n");
  }

  @Test
  void testApplyCodeActionWithMultipleTextEdits1() {
    final String source = "Hello, world!\n";
    final CodeActionApplier codeActionApplier = new CodeActionApplier(source);
    codeActionApplier.apply(
        new CodeAction(
            "test fix",
            List.of(
                new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "New line!\n"),
                new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "New line!\n"))));
    final String newSource = codeActionApplier.getSource();
    assertThat(newSource).isEqualTo("New line!\nNew line!\nHello, world!\n");
  }

  @Test
  void testApplyCodeActionWithMultipleTextEdits2() {
    final String source = "Hello, world!\n";
    final CodeActionApplier codeActionApplier = new CodeActionApplier(source);
    codeActionApplier.apply(
        new CodeAction(
            "test fix",
            List.of(
                new TextEdit(new Range(new Position(2, 1), new Position(2, 1)), "New line!\n"),
                new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "New line!\n"))));
    final String newSource = codeActionApplier.getSource();
    assertThat(newSource).isEqualTo("New line!\nHello, world!\nNew line!\n");
  }
}
