package nl.ramsolutions.sw.magik.languageserver.linkededitingrange;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/** Tests for {@link LinkedEditingRangeProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class LinkedEditingRangeProviderTest {

  private LinkedEditingRanges provideLinkedEditingRanges(
      final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final LinkedEditingRangeProvider provider = new LinkedEditingRangeProvider();
    return provider.provideLinkedEditingRanges(magikFile, position);
  }

  @Test
  void testLocalVariableDefinitionAndUsage() {
    final String code =
        """
        _method object.m()
          _local x << 1
          _return x
        _endmethod
        """;
    // Cursor on definition of `x` (line 1, col 9).
    final LinkedEditingRanges ranges = this.provideLinkedEditingRanges(code, new Position(1, 9));
    assertThat(ranges).isNotNull();
    // Should contain range for definition + one usage.
    assertThat(ranges.getRanges()).hasSize(2);
  }

  @Test
  void testParameterDefinitionAndUsage() {
    final String code =
        """
        _method object.m(p1)
          _return p1
        _endmethod
        """;
    // Cursor on `p1` in the parameter list (line 0, col 17).
    final LinkedEditingRanges ranges = this.provideLinkedEditingRanges(code, new Position(0, 17));
    assertThat(ranges).isNotNull();
    assertThat(ranges.getRanges()).hasSize(2);
  }

  @Test
  void testGlobalVariableReturnsNull() {
    final String code =
        """
        _method object.m()
          _global g
        _endmethod
        """;
    // Cursor on `g` (line 1, col 10).
    final LinkedEditingRanges ranges = this.provideLinkedEditingRanges(code, new Position(1, 10));
    assertThat(ranges).isNull();
  }

  @Test
  void testCursorNotOnIdentifierReturnsNull() {
    final String code =
        """
        _method object.m()
          _return 1
        _endmethod
        """;
    // Cursor on `_method` keyword (line 0, col 0).
    final LinkedEditingRanges ranges = this.provideLinkedEditingRanges(code, new Position(0, 0));
    assertThat(ranges).isNull();
  }
}
