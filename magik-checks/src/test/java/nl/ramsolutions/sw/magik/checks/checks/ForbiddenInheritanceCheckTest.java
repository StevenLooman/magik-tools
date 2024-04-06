package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Tests for ForbiddenInheritanceCheck. */
class ForbiddenInheritanceCheckTest extends MagikCheckTestBase {

  @Test
  void testParentOk() {
    final ForbiddenInheritanceCheck check = new ForbiddenInheritanceCheck();
    check.forbiddenParents = "user:rwo_record";
    final String code =
        """
        def_slotted_exemplar(
            :test_exemplar,
            {})""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testParentForbidden() {
    final ForbiddenInheritanceCheck check = new ForbiddenInheritanceCheck();
    check.forbiddenParents = "sw:rwo_record,user:rwo_record,sw:ds_record,user:ds_record";
    final String code =
        """
        def_slotted_exemplar(
            :test_exemplar,
            {},
            :rwo_record)""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testParentForbidden2() {
    final ForbiddenInheritanceCheck check = new ForbiddenInheritanceCheck();
    check.forbiddenParents = "sw:rwo_record, user:rwo_record, sw:ds_record, user:ds_record";
    final String code =
        """
        def_slotted_exemplar(
            :test_exemplar,
            {},
            {@sw:rwo_record})""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
