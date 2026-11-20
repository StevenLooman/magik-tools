package nl.ramsolutions.sw.magik.languageserver.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Tests for {@link MethodNameReferencesModule} over parsed sources. */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodNameReferencesModuleTest {

  @RegisterExtension final SmallworldProjectExtension extension = new SmallworldProjectExtension();

  @Test
  void testReferencesAnswerInvokesMethodAnnotationOnMethod() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.caller(name)
          ## @invokes_method {user:a.method_name}
          _self.perform(name)
        _endmethod
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactly(4);
  }

  @Test
  void testReferencesAnswerInvokesMethodAnnotationOnProcedure() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global p << _proc@p(name)
          ## @invokes_method {user:a.method_name}
          a.perform(name)
        _endproc
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactly(4);
  }

  @Test
  void testReferencesAnswerCallInTopLevelProcedure() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global p << _proc@p()
          a.method_name
        _endproc
        q << _proc@q()
          a.method_name
        _endproc
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactlyInAnyOrder(5, 8);
  }

  @Test
  void testReferencesAnswerCallInProcedureInMethodOnce() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _method a.runner
          _return _proc@p()
            a.method_name
          _endproc
        _endmethod
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactly(6);
  }

  @Test
  void testReferencesAnswerCallInGlobalInitialiser() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global g << a.method_name
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactly(4);
  }

  @Test
  void testReferencesAnswerCallInProcedureInGlobalInitialiserOnce() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})
        _method a.method_name
        _endmethod
        _global g << a.wrap(_proc@p()
          a.method_name
        _endproc)
        """;

    final List<Integer> lines = this.findReferenceLines(code);

    assertThat(lines).containsExactly(5);
  }

  /** Find the lines of the references to the method defined at line 2. */
  private List<Integer> findReferenceLines(final String code) throws IOException {
    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final Position position = new Position(2, 12); // On `method_name`.
    final ReferencesProvider provider = new ReferencesProvider();
    final List<Location> references = provider.provideReferences(magikFile, position);
    return references.stream().map(this::getStartLine).toList();
  }

  private int getStartLine(final Location location) {
    final Range range = location.getRange();
    final Position startPosition = range.getStartPosition();
    return startPosition.getLine();
  }
}
