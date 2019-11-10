package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class DuplicateMethodInFileCheckTest extends MagikCheckTestBase {

  @Test
  public void testNoParameters() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a\n" +
        "_endmethod\n" +
        "_method a.b\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoParametersDuplicate() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a\n" +
        "_endmethod\n" +
        "_method a.a\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @Test
  public void testParameters() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a(p1)\n" +
        "_endmethod\n" +
        "_method a.a\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(0);
  }

  @Test
  public void testParametersDuplicate() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a(p1)\n" +
        "_endmethod\n" +
        "_method a.a(p1, p2)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @Test
  public void testParametersAssignment() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a(p1)\n" +
        "_endmethod\n" +
        "_method a.a(p1, p2) << p3\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(0);
  }

  @Test
  public void testParametersAssignmentDuplicate() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a.a(p1) << p2\n" +
        "_endmethod\n" +
        "_method a.a(p1) << p2\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @Test
  public void testIndexer() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a[p1]\n" +
        "_endmethod\n" +
        "_method a[p1, p2]\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(0);
  }

  @Test
  public void testIndexerDuplicate() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a[p1]\n" +
        "_endmethod\n" +
        "_method a[p1]\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @Test
  public void testIndexerAssignment() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a[p1]\n" +
        "_endmethod\n" +
        "_method a[p1] << p2\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(0);
  }

  @Test
  public void testIndexerAssignmentDuplicate() {
    MagikCheck check = new DuplicateMethodInFileCheck();
    String code =
        "_method a[p1] << p2\n" +
        "_endmethod\n" +
        "_method a[p1] << p2\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

}
