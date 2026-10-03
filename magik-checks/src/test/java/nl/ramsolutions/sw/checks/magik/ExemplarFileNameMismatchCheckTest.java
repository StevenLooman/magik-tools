package nl.ramsolutions.sw.checks.magik;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Test {@link ExemplarFileNameMismatchCheck}. */
class ExemplarFileNameMismatchCheckTest {

  private List<Issue> runCheck(
      final ExemplarFileNameMismatchCheck check, final String fileName, final String code) {
    final URI uri = URI.create("file:///project/source/" + fileName);
    final OpenedFile openedFile = new MagikFile(uri, code);
    return check.scanFileForIssues(openedFile);
  }

  @Test
  void testExemplarDefinitionInMatchingFile() {
    final String code = "def_slotted_exemplar(:my_exemplar, {})";
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "my_exemplar.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testExemplarDefinitionInNonMatchingFile() {
    final String code = "def_slotted_exemplar(:my_exemplar, {})";
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodDefinitionInMatchingFile() {
    final String code =
        """
        _method my_exemplar.foo()
            _return 42
        _endmethod
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "my_exemplar.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMethodDefinitionInMatchingFileWithSuffix() {
    final String code =
        """
        _method my_exemplar.foo()
            _return 42
        _endmethod
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "my_exemplar_adds.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMethodDefinitionInNonMatchingFile() {
    final String code =
        """
        _method my_exemplar.foo()
            _return 42
        _endmethod
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testDefaultExceptionsRecordExemplars() {
    final String code =
        """
        def_slotted_exemplar(:my_exemplar, {})
        _method my_exemplar.foo()
            _return 42
        _endmethod
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "record_exemplars.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDefaultExceptionsGlue() {
    final String code =
        """
        def_slotted_exemplar(:my_exemplar, {})
        _method my_exemplar.foo()
            _return 42
        _endmethod
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "glue.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testCustomExceptions() {
    final String code = "def_slotted_exemplar(:my_exemplar, {})";
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    check.exceptions = "custom_exceptions.magik, other_exception.magik";
    final List<Issue> issues = this.runCheck(check, "custom_exceptions.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureNotInCheck() {
    final String code =
        """
        _global my_proc << _proc()
            _return 42
        _endproc
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testExemplarWithSlotsInNonMatchingFileReportsOnce() {
    final String code =
        """
        def_slotted_exemplar(:my_exemplar,
            {{:count, 0, :read, :public},
             {:name, _unset, :write, :public}})
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSlotAccessorViaDefineSlotAccessNotChecked() {
    final String code =
        """
        other_exemplar.define_slot_access(:count, :read, :public)
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSharedConstantNotChecked() {
    final String code =
        """
        other_exemplar.define_shared_constant(:max_count, 100, :public)
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSharedVariableNotChecked() {
    final String code =
        """
        other_exemplar.define_shared_variable(:count, 0, :public)
        """;
    final ExemplarFileNameMismatchCheck check = new ExemplarFileNameMismatchCheck();
    final List<Issue> issues = this.runCheck(check, "other_file.magik", code);
    assertThat(issues).isEmpty();
  }
}
