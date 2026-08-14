package nl.ramsolutions.sw.magik.languageserver.codeactions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.junit.jupiter.api.Test;

/** Tests for {@link CodeActionProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class CodeActionProviderTest {

  private static final String REPLACE_IS_TITLE = "Replace `_is` operator with `=` operator";
  private static final String EXTRACT_METHOD_TITLE = "Extract to method";
  private static final String EXTRACT_LOCAL_VARIABLE_TITLE = "Extract to local variable";

  // Line 1: `_method my_object.test_method()`
  // Line 2: `    a _is "b"`         -- triggers UseValueCompare, fixer edit is the `_is` token,
  //                                    columns 6-9; the full issue spans columns 4-13.
  // Line 3: `    write(2)`          -- literal `2` at columns 10-11, extractable.
  // Line 4: `_endmethod`
  // Line 5: `$`
  private static final String CODE =
      """
      _method my_object.test_method()
          a _is "b"
          write(2)
      _endmethod
      $
      """;

  private MagikTypedFile createMagikFile() {
    return new MagikTypedFile(
        MagikToolsProperties.DEFAULT_PROPERTIES,
        MagikTypedFile.DEFAULT_URI,
        CODE,
        new DefinitionKeeper());
  }

  private CodeActionProvider createProvider() {
    return new CodeActionProvider(MagikToolsProperties.DEFAULT_PROPERTIES);
  }

  private List<CodeAction> provideCodeActions(final Range range, final CodeActionContext context) {
    return this.createProvider().provideCodeActions(this.createMagikFile(), range, context);
  }

  @Test
  void testChecksBasedFixContributesResult() {
    // Range around the full `a _is "b"` issue, so the fixer's edit (the `_is` token) is in range.
    final Range range = new Range(new Position(2, 4), new Position(2, 13));
    final CodeActionContext context = new CodeActionContext(Collections.emptyList());

    final List<CodeAction> actions = this.provideCodeActions(range, context);

    assertThat(actions).anyMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
  }

  @Test
  void testExtractActionContributesResult() {
    // Select just the literal `2` on line 3.
    final Range range = new Range(new Position(3, 10), new Position(3, 11));
    final CodeActionContext context = new CodeActionContext(Collections.emptyList());

    final List<CodeAction> actions = this.provideCodeActions(range, context);

    assertThat(actions).anyMatch(action -> action.getTitle().equals(EXTRACT_LOCAL_VARIABLE_TITLE));
  }

  @Test
  void testChecksAndExtractResultsAreCombined() {
    // Range covering both full statements: the checks fix on line 2 and an extractable
    // statement on line 3.
    final Range range = new Range(new Position(2, 4), new Position(3, 12));
    final CodeActionContext context = new CodeActionContext(Collections.emptyList());

    final List<CodeAction> actions = this.provideCodeActions(range, context);

    assertThat(actions).anyMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
    assertThat(actions).anyMatch(action -> action.getTitle().equals(EXTRACT_METHOD_TITLE));
  }

  @Test
  void testOnlyKindFilterExcludesNonMatchingContributor() {
    final Range range = new Range(new Position(2, 4), new Position(3, 12));

    final CodeActionContext extractOnlyContext =
        new CodeActionContext(Collections.emptyList(), List.of(CodeActionKind.RefactorExtract));
    final List<CodeAction> extractOnlyActions = this.provideCodeActions(range, extractOnlyContext);
    assertThat(extractOnlyActions).noneMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
    assertThat(extractOnlyActions)
        .anyMatch(action -> action.getTitle().equals(EXTRACT_METHOD_TITLE));

    final CodeActionContext quickfixOnlyContext =
        new CodeActionContext(Collections.emptyList(), List.of(CodeActionKind.QuickFix));
    final List<CodeAction> quickfixOnlyActions =
        this.provideCodeActions(range, quickfixOnlyContext);
    assertThat(quickfixOnlyActions).anyMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
    assertThat(quickfixOnlyActions)
        .noneMatch(action -> action.getTitle().equals(EXTRACT_METHOD_TITLE));
  }

  @Test
  void testRangeOverlapFilterAppliesToChecksButNotExtractStream() throws Exception {
    // Range covering only the `"b"` literal (columns 10-13), which does NOT overlap the `_is`
    // token (columns 6-9) that the checks fixer's edit targets, but DOES overlap the full
    // `a _is "b"` issue range (columns 4-13), so the fixer itself still produces the action.
    final Range range = new Range(new Position(2, 10), new Position(2, 13));
    final CodeActionContext context = new CodeActionContext(Collections.emptyList());

    final List<CodeAction> actions = this.provideCodeActions(range, context);

    // Dropped: CodeActionProvider's own range-overlap filter applies to the checks stream.
    assertThat(actions).noneMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
    // Kept: the extract stream is not range-filtered, so an action targeting the same range
    // (the `"b"` literal) survives.
    assertThat(actions).anyMatch(action -> action.getTitle().equals(EXTRACT_LOCAL_VARIABLE_TITLE));

    // Confirm the checks sub-provider itself DID produce the fix for this range: it is
    // CodeActionProvider's own filter -- not the fixer -- that drops it.
    final ChecksCodeActionProvider checksProvider =
        new ChecksCodeActionProvider(
            MagikCheckList.INSTANCE, MagikToolsProperties.DEFAULT_PROPERTIES);
    final List<CodeAction> checksActions =
        checksProvider.provideCodeActions(this.createMagikFile(), range);
    assertThat(checksActions).anyMatch(action -> action.getTitle().equals(REPLACE_IS_TITLE));
  }
}
