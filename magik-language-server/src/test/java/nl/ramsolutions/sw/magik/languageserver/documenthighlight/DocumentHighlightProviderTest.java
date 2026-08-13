package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/** Tests for {@link DocumentHighlightProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class DocumentHighlightProviderTest {

  private List<DocumentHighlight> provideHighlights(final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final DocumentHighlightProvider provider = new DocumentHighlightProvider();
    return provider.provideDocumentHighlights(magikFile, position);
  }

  @Test
  void testLocalVariableWriteAndRead() {
    final String code =
        """
        _method object.m()
          _local x << 1
          _return x
        _endmethod
        """;
    // Cursor on definition of `x` (line 1, col 9).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(1, 9));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Write));
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Read));
  }

  @Test
  void testLocalVariableOnUsage() {
    final String code =
        """
        _method object.m()
          _local x << 1
          _return x
        _endmethod
        """;
    // Cursor on usage of `x` (line 2, col 10).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(2, 10));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Write));
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Read));
  }

  @Test
  void testParameterHighlight() {
    final String code =
        """
        _method object.m(p1)
          _return p1
        _endmethod
        """;
    // Cursor on parameter `p1` in definition (line 0, col 17).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(0, 17));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Write));
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Read));
  }

  @Test
  void testMethodNameHighlight() {
    final String code =
        """
        _method integer.my_method()
          _return _self.my_method()
        _endmethod
        """;
    // Cursor on method name in definition (line 0, col 17).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(0, 17));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testMethodInvocationHighlight() {
    final String code =
        """
        _method integer.my_method()
          _return _self.my_method()
        _endmethod
        """;
    // Cursor on method name in invocation (line 1, col 17).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(1, 17));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testCursorNotOnIdentifierReturnsEmpty() {
    final String code =
        """
        _method object.m()
          _return 1
        _endmethod
        """;
    // Cursor on `_method` keyword (line 0, col 0).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(0, 0));
    assertThat(highlights).isEmpty();
  }

  @Test
  void testSlotHighlight() {
    final String code =
        """
        _method object.m()
          .my_slot << 1
          _return .my_slot
        _endmethod
        """;
    // Cursor on slot write (line 1, col 3).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(1, 3));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testExemplarNameHighlight() {
    final String code =
        """
        _method object.m1()
          _return 1
        _endmethod

        _method object.m2()
          _return 2
        _endmethod
        """;
    // Cursor on exemplar name of the first method definition (line 0, col 9).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(0, 9));
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testConditionNameHighlight() {
    final String code =
        """
        _method object.m()
          _try
          _when error
            _return 1
          _endtry
        _endmethod
        """;
    // Cursor on condition name in `_when` (line 2, col 9).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(2, 9));
    assertThat(highlights).hasSize(1);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testGlobalVariableHighlight() {
    final String code =
        """
        _global my_global << 1

        _method object.m()
          _return my_global
        _endmethod
        """;
    // Cursor on usage of the global (line 3, col 11).
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(3, 11));
    assertThat(highlights).hasSize(1);
    assertThat(highlights)
        .allSatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Text));
  }

  @Test
  void testScopeEntryPrecedesSlotAncestor() {
    final String code =
        """
        _method object.m(p1)
          .p1 << p1
          _return .p1
        _endmethod
        """;
    // Cursor on the slot identifier of `.p1 <<` (line 1, col 4), which shares its name with
    // parameter `p1`.
    final List<DocumentHighlight> highlights = this.provideHighlights(code, new Position(1, 4));
    // Stage 1 (scope entry) preempts stage 2 (slot ancestor): resolves as the parameter, not
    // the slot.
    assertThat(highlights).hasSize(2);
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Write));
    assertThat(highlights)
        .anySatisfy(h -> assertThat(h.getKind()).isEqualTo(DocumentHighlightKind.Read));
  }
}
