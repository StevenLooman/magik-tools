package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Tests for {@link AnonymousNamer}. */
class AnonymousNamerTest {

  /**
   * The anonymous procedure name embeds the file path. A path can contain characters that are
   * significant in {@link TypeString} syntax: {@code ':'} (the package separator, present in every
   * Windows absolute path via the drive letter, e.g. {@code D:\...}), {@code '|'} (combinator),
   * {@code '<'}/{@code '>'} (generics) and {@code ','}. Any of these leaking into the identifier is
   * mis-parsed, and the resulting type can no longer be resolved back to the procedure.
   *
   * <p>Enabled only on Linux: this injects the characters through a path, and Windows rejects them
   * as illegal path characters ({@code Path.of} throws). On Linux they are legal in file names, so
   * the path round-trips and exercises the sanitization. The Windows drive-letter {@code ':'} is
   * covered separately by {@link #testProcedureNameSanitizesDriveLetterColon()}.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void testProcedureNameSanitizesTypeStringCharsInPath() {
    final URI uri = Path.of("/tmp/a:b|c<d>e,f/test.magik").toUri();

    this.assertProcedureNameIsSanitized(uri);
  }

  /**
   * Every Windows absolute path carries a drive-letter {@code ':'} (e.g. {@code D:\...}), which is
   * the {@link TypeString} package separator. This is the exact scenario that leaked into anonymous
   * procedure names before sanitization; the mid-path colon of {@link
   * #testProcedureNameSanitizesTypeStringCharsInPath()} cannot be expressed on Windows (it rejects
   * {@code ':'} in path segments), so this test pins the drive-letter case on the platform where it
   * actually occurs.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  void testProcedureNameSanitizesDriveLetterColon() {
    final URI uri = Path.of("C:\\tmp\\test.magik").toUri();

    this.assertProcedureNameIsSanitized(uri);
  }

  private void assertProcedureNameIsSanitized(final URI uri) {
    final MagikFile magikFile =
        new MagikFile(
            uri,
            """
            _method object.method()
                _return _proc() _endproc
            _endmethod
            """);
    final AstNode procNode =
        magikFile.getTopNode().getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);

    final TypeString typeStr = AnonymousNamer.getNameForProcedure(procNode);

    // The type must remain a single identifier in the `_anon` package, with a `_proc_...`
    // identifier made only of identifier-safe characters. If a path character leaked in, the type
    // would be mis-parsed (e.g. ':' splits off a bogus package, '|' makes it look combined).
    assertThat(typeStr.isSingle()).isTrue();
    assertThat(typeStr.getPakkage()).isEqualTo(TypeString.ANONYMOUS_PACKAGE);
    assertThat(typeStr.getIdentifier()).startsWith("_proc_").matches("[A-Za-z0-9_]+");
  }
}
