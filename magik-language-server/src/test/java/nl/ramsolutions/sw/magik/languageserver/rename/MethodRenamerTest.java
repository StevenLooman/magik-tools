package nl.ramsolutions.sw.magik.languageserver.rename;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MethodRenamerTest {

  /** {@code method_name()} on {@code sw:a}, once under its own name and once written in user. */
  private static final String SPELLED_METHOD_CODE =
      """
      _package sw
      def_slotted_exemplar(:a, {})
      $
      _method a.method_name()
      _endmethod
      $
      _package user
      _method a.method_name()
      _endmethod
      $
      _method a.caller()
        _self.method_name()
      _endmethod
      $
      """;

  private static final String SPELLED_METHOD_RENAMED_CODE =
      """
      _package sw
      def_slotted_exemplar(:a, {})
      $
      _method a.new_method_name()
      _endmethod
      $
      _package user
      _method a.new_method_name()
      _endmethod
      $
      _method a.caller()
        _self.new_method_name()
      _endmethod
      $
      """;

  @RegisterExtension final SmallworldProjectExtension extension = new SmallworldProjectExtension();

  @Test
  void testPrepareRenameOnDefinition() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(1, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(1, 10), new Position(1, 21))),
                "method_name"));
  }

  @Test
  void testPrepareRenameOnInvocation() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(2, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(2, 8), new Position(2, 19))),
                "method_name"));
  }

  @Test
  void testRenameMethodSelf() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(3, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:test:///source.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(3, 10), new Position(3, 21)), "new_method_name"),
                    new TextEdit(
                        new Range(new Position(4, 8), new Position(4, 19)), "new_method_name"))));
  }

  @Test
  void testRenameMethodRewritesInvokesMethodAnnotation() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _method a.caller(name)
          ## @invokes_method {user:a.method_name()}
          _self.perform(name)
        _endmethod

        _method a.real_caller()
          _self.method_name()
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _method a.caller(name)
          ## @invokes_method {user:a.new_method_name()}
          _self.perform(name)
        _endmethod

        _method a.real_caller()
          _self.new_method_name()
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesInvokesMethodAnnotationForms() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name
        _endmethod

        _method a.caller(name)
          ## Calls by name.
          ## @invokes_method {a.method_name}
          ## @invokes_method {user:a.method_name}
          ## @invokes_method {a.method_name}
          ## @invokes_method {a.method_name()}
          ## @invokes_method {a[]}
          ## @invokes_method {a[method_name]}
          ## @return {a.method_name} Not an invocation.
          _self.perform(name)
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name
        _endmethod

        _method a.caller(name)
          ## Calls by name.
          ## @invokes_method {a.new_method_name}
          ## @invokes_method {user:a.new_method_name}
          ## @invokes_method {a.new_method_name}
          ## @invokes_method {a.method_name()}
          ## @invokes_method {a[]}
          ## @invokes_method {a[method_name]}
          ## @return {a.method_name} Not an invocation.
          _self.perform(name)
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesTabIndentedInvokesMethodAnnotation() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _method a.caller(name)
        \t\t## @invokes_method {a.method_name()}
        \t\t_self.perform(name)
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _method a.caller(name)
        \t\t## @invokes_method {a.new_method_name()}
        \t\t_self.perform(name)
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodLeavesInvokesMethodAnnotationOnOtherReceiver() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        def_slotted_exemplar(:b, {})

        _method b.caller(name)
          ## @invokes_method {user:b.method_name()}
          _self.perform(name)
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        def_slotted_exemplar(:b, {})

        _method b.caller(name)
          ## @invokes_method {user:b.method_name()}
          _self.perform(name)
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesInvokesMethodAnnotationOnProcedure() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _global p << _proc@p(name)
          ## @invokes_method {user:a.method_name()}
          a.perform(name)
        _endproc
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _global p << _proc@p(name)
          ## @invokes_method {user:a.new_method_name()}
          a.perform(name)
        _endproc
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesCallInTopLevelProcedure() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _global p << _proc@p()
          a.method_name()
        _endproc
        q << _proc@q()
          a.method_name()
        _endproc
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _global p << _proc@p()
          a.new_method_name()
        _endproc
        q << _proc@q()
          a.new_method_name()
        _endproc
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesCallInGlobalInitialiser() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _global g << a.method_name()
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _global g << a.new_method_name()
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRewritesCallInProcedureInMethodOnce() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _method a.runner()
          _return _proc@p()
            a.method_name()
          _endproc
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _method a.runner()
          _return _proc@p()
            a.new_method_name()
          _endproc
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodLeavesCallEnclosingAnnotatedProcedureArgument() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
        _endmethod

        _method a.do(callback)
        _endmethod

        _method a.runner()
          _self.do(_proc@p(name)
            ## @invokes_method {user:a.method_name()}
          _endproc)
        _endmethod
        """;

    final String renamed = this.renameMethodName(code);

    final String expected =
        """
        def_slotted_exemplar(:a, {})

        _method a.new_method_name()
        _endmethod

        _method a.do(callback)
        _endmethod

        _method a.runner()
          _self.do(_proc@p(name)
            ## @invokes_method {user:a.new_method_name()}
          _endproc)
        _endmethod
        """;
    assertThat(renamed).isEqualTo(expected);
  }

  @Test
  void testRenameMethodRenamesDefinitionWrittenInUsingPackage() throws IOException {
    final Position position = new Position(4, 10); // On `method_name` of sw:a.

    final String renamed = this.renameMethodNameAt(SPELLED_METHOD_CODE, position);

    assertThat(renamed).isEqualTo(SPELLED_METHOD_RENAMED_CODE);
  }

  @Test
  void testRenameMethodFromDefinitionWrittenInUsingPackage() throws IOException {
    final Position position = new Position(8, 10); // On `method_name` of user:a.

    final String renamed = this.renameMethodNameAt(SPELLED_METHOD_CODE, position);

    assertThat(renamed).isEqualTo(SPELLED_METHOD_RENAMED_CODE);
  }

  /** Rename the method at line 3 to {@code new_method_name} and apply the edits to the code. */
  private String renameMethodName(final String code) throws IOException {
    final Position position = new Position(3, 10); // On `method_name`.
    return this.renameMethodNameAt(code, position);
  }

  /** As {@link #renameMethodName}, from the given position. */
  private String renameMethodNameAt(final String code, final Position position) throws IOException {
    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");

    final URI uri = URI.create("memory:test:///source.magik");
    assertThat(renames).containsOnlyKeys(uri);
    final List<TextEdit> edits = renames.get(uri);
    return this.applyEdits(code, edits);
  }

  /** Apply single-line edits to the code, last edit first. Fails on overlapping edits. */
  private String applyEdits(final String code, final List<TextEdit> edits) {
    final Comparator<TextEdit> byStart = Comparator.comparing(this::getStartPosition);
    final List<TextEdit> sortedEdits = edits.stream().sorted(byStart).toList();
    final List<String> lines = new ArrayList<>(code.lines().toList());
    Position nextStart = null;
    for (int i = sortedEdits.size() - 1; i >= 0; --i) {
      final TextEdit edit = sortedEdits.get(i);
      final Range range = edit.getRange();
      final Position start = range.getStartPosition();
      final Position end = range.getEndPosition();
      if (nextStart != null) {
        assertThat(end).isLessThanOrEqualTo(nextStart);
      }
      nextStart = start;

      final int lineIndex = start.getLine() - 1;
      final String line = lines.get(lineIndex);
      final String head = line.substring(0, start.getColumn());
      final String tail = line.substring(end.getColumn());
      final String newLine = head + edit.getNewText() + tail;
      lines.set(lineIndex, newLine);
    }

    final String text = String.join("\n", lines);
    return text + "\n";
  }

  private Position getStartPosition(final TextEdit edit) {
    final Range range = edit.getRange();
    return range.getStartPosition();
  }

  @Test
  void testRenameMethodMultiFile() throws IOException {
    final String codeA =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final String codeB =
        """
        _method b.method()
          a.method_name()
        _endmethod
        """;
    final Position position = new Position(3, 10); // On `method_name`.

    final Path pathA = this.extension.pathOf("/source_a.magik");
    final MagikTypedFile magikFileA = this.extension.addMagikFile(pathA, codeA);
    final Path pathB = this.extension.pathOf("/source_b.magik");
    this.extension.addMagikFile(pathB, codeB);

    final AstNode topNode = magikFileA.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFileA, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:test:///source_a.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(3, 10), new Position(3, 21)), "new_method_name"),
                    new TextEdit(
                        new Range(new Position(4, 8), new Position(4, 19)), "new_method_name")),
                URI.create("memory:test:///source_b.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(2, 4), new Position(2, 15)), "new_method_name"))));
  }

  @Test
  void testRenameMethodLeavesSuperCallToParent() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:p, {})
        def_slotted_exemplar(:t, {}, {:p})

        _method p.method_name()
        _endmethod

        _method t.method_name()
          _super.method_name()
        _endmethod
        """;
    final Position position = new Position(7, 10); // On `method_name` of `t`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");

    // `_super.method_name()` calls `p`'s method, which is not renamed.
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:test:///source.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(7, 10), new Position(7, 21)), "new_method_name"))));
  }

  /** A file deleted after the keeper was filled gives no edits, and does not fail the rename. */
  @Test
  void testRenameMethodSkipsCallInDeletedFile() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final String deletedCode =
        """
        _method a.caller()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(3, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final Path deletedPath = this.extension.pathOf("/deleted.magik");
    this.extension.addMagikFile(deletedPath, deletedCode);
    Files.delete(deletedPath);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");

    final Set<URI> renamedUris = renames.keySet();
    final URI uri = path.toUri();
    assertThat(renamedUris).containsExactly(uri);
  }
}
