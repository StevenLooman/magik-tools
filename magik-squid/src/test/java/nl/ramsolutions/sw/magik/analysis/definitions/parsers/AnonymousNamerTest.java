package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link AnonymousNamer}. */
class AnonymousNamerTest {

  private static final String METHOD_CODE =
      """
      _package sw
      _method object.method()
          _return _proc() _endproc
      _endmethod
      """;

  private List<AstNode> getProcedureNodes(final URI uri, final String code) {
    final MagikFile magikFile = new MagikFile(uri, code);
    final AstNode topNode = magikFile.getTopNode();
    return topNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION);
  }

  private TypeString getProcedureName(final URI uri, final String code, final int index) {
    final List<AstNode> procedureNodes = this.getProcedureNodes(uri, code);
    final AstNode procedureNode = procedureNodes.get(index);
    return AnonymousNamer.getNameForProcedure(procedureNode);
  }

  @Test
  void testProcedureNameIndependentOfPath() {
    final URI tmpUri = Path.of("/tmp/munit/source/test.magik").toUri();
    final URI workspaceUri = Path.of("/workspaces/munit/source/test.magik").toUri();

    final TypeString tmpName = this.getProcedureName(tmpUri, METHOD_CODE, 0);
    final TypeString workspaceName = this.getProcedureName(workspaceUri, METHOD_CODE, 0);

    assertThat(tmpName).isEqualTo(workspaceName);
  }

  @Test
  void testProcedureNameInMethod() {
    final TypeString name = this.getProcedureName(MagikFile.DEFAULT_URI, METHOD_CODE, 0);

    final TypeString expected =
        TypeString.ofIdentifier("_proc__sw__object__method____0", TypeString.ANONYMOUS_PACKAGE);
    assertThat(name).isEqualTo(expected);
  }

  @Test
  void testProcedureNameIndexedWithinMethod() {
    final String code =
        """
        _package sw
        _method object.method()
            _local a << _proc() _endproc
            _return _proc() _return _proc() _endproc _endproc
        _endmethod
        """;

    final TypeString name = this.getProcedureName(MagikFile.DEFAULT_URI, code, 2);

    final TypeString expected =
        TypeString.ofIdentifier("_proc__sw__object__method____2", TypeString.ANONYMOUS_PACKAGE);
    assertThat(name).isEqualTo(expected);
  }

  @Test
  void testProcedureNameUnaffectedByProcedureInOtherMethod() {
    final String before =
        """
        _package sw
        _method object.a()
        _endmethod

        _method object.b()
            _return _proc() _endproc
        _endmethod
        """;
    final String after =
        """
        _package sw
        _method object.a()
            _return _proc() _endproc
        _endmethod

        _method object.b()
            _return _proc() _endproc
        _endmethod
        """;

    final TypeString beforeName = this.getProcedureName(MagikFile.DEFAULT_URI, before, 0);
    final TypeString afterName = this.getProcedureName(MagikFile.DEFAULT_URI, after, 1);

    assertThat(afterName).isEqualTo(beforeName);
  }

  @Test
  void testProcedureNameInNamedProcedure() {
    final String code =
        """
        _package user
        _global my_proc << _proc()
            _return _proc() _endproc
        _endproc
        """;

    final TypeString name = this.getProcedureName(MagikFile.DEFAULT_URI, code, 1);

    final TypeString expected =
        TypeString.ofIdentifier("_proc__user__my_proc__0", TypeString.ANONYMOUS_PACKAGE);
    assertThat(name).isEqualTo(expected);
  }

  @Test
  void testFileLevelProcedureNameUsesModuleAndFile(@TempDir final Path tempDir) throws IOException {
    final Path moduleDefPath = tempDir.resolve("module.def");
    Files.writeString(moduleDefPath, "my_module 1\n");
    final Path sourcePath = tempDir.resolve("source/my_file.magik");
    final URI uri = sourcePath.toUri();
    final String code =
        """
        _package sw
        _method object.method()
            _return _proc() _endproc
        _endmethod

        _block
            _local p << _proc() _endproc
        _endblock
        """;

    final TypeString name = this.getProcedureName(uri, code, 1);

    final TypeString expected =
        TypeString.ofIdentifier("_proc__my_module__my_file_magik__0", TypeString.ANONYMOUS_PACKAGE);
    assertThat(name).isEqualTo(expected);
  }

  @Test
  void testFileLevelProcedureNameWithoutModule() {
    final String code =
        """
        _package sw
        _block
            _local p << _proc() _endproc
            _local q << _proc() _endproc
        _endblock
        """;

    final TypeString name = this.getProcedureName(MagikFile.DEFAULT_URI, code, 1);

    final TypeString expected =
        TypeString.ofIdentifier("_proc__in_memory__1", TypeString.ANONYMOUS_PACKAGE);
    assertThat(name).isEqualTo(expected);
  }

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
            _block
                _return _proc() _endproc
            _endblock
            """);
    final AstNode procNode =
        magikFile.getTopNode().getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);

    final TypeString typeStr = AnonymousNamer.getNameForProcedure(procNode);
    final String identifier = typeStr.getIdentifier();

    // The type must remain a single identifier in the `_anon` package, with a `_proc_...`
    // identifier made only of identifier-safe characters. If a path character leaked in, the type
    // would be mis-parsed (e.g. ':' splits off a bogus package, '|' makes it look combined).
    assertThat(typeStr.isSingle()).isTrue();
    assertThat(typeStr.getPakkage()).isEqualTo(TypeString.ANONYMOUS_PACKAGE);
    assertThat(identifier).startsWith("_proc_").matches("[A-Za-z0-9_]+");
  }
}
