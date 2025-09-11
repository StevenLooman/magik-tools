package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests for {@link ForbiddenInheritanceCheck}. */
class ForbiddenInheritanceCheckTest {

  @Test
  void testParentOk() {
    final ForbiddenInheritanceCheck check = new ForbiddenInheritanceCheck();
    check.forbiddenParents = "user:rwo_record";
    final String code =
        """
        def_slotted_exemplar(
          :test_exemplar,
          {})""";
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 1);
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
    assertThat(check).reportsIssueCount(code, 1);
  }
}
