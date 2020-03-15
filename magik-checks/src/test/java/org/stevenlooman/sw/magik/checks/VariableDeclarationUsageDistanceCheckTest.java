package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class VariableDeclarationUsageDistanceCheckTest extends MagikCheckTestBase {

  @Test
  public void testDistanceOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    String code =
      "_method a.b\n" +
      "  _local a << 1\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  write(a)\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDistanceNotOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    String code =
      "_method a.b\n" +
      "  _local a << 1\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  write(a)\n" +
      "  write(a)\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testUpperScopeDistanceOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    String code =
      "_method a.b\n" +
      "  _local a << 1\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testUpperScopeDistanceNotOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    String code =
      "_method a.b\n" +
      "  _local a << 1\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testDifferentScopeDistanceOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    String code =
      "_method a.b\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    a << 1\n" +
      "  _endif\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDifferentScopeDistanceNotOk() {
    VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    String code =
      "_method a.b\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    a << 1\n" +
      "  _endif\n" +
      "  do_something()\n" +
      "  do_something()\n" +
      "  _if _true\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

}