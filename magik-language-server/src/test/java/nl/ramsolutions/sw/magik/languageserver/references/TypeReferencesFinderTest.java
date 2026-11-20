package nl.ramsolutions.sw.magik.languageserver.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Test {@link TypeReferencesFinder}. */
class TypeReferencesFinderTest {

  @RegisterExtension final SmallworldProjectExtension extension = new SmallworldProjectExtension();

  private int getStartLine(final Location location) {
    final Range range = location.getRange();
    final Position startPosition = range.getStartPosition();
    return startPosition.getLine();
  }

  @Test
  void testReferencesAnswerTypeUsedUnderUsingPackagesName() throws IOException {
    final String code =
        """
        _package sw
        def_slotted_exemplar(:a, {})
        $
        _package user
        _method b.m()
          >> a
        _endmethod
        $
        """;
    final Path path = this.extension.pathOf("/source.magik");
    this.extension.addMagikFile(path, code);
    final IDefinitionKeeper keeper = this.extension.getDefinitionKeeper();

    final List<Location> references =
        TypeReferencesFinder.referencesToType(keeper, TypeString.ofIdentifier("a", "sw"));

    final List<Integer> lines = references.stream().map(this::getStartLine).toList();
    assertThat(lines).containsExactly(6);
  }
}
