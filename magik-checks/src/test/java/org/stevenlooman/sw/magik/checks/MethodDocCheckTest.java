package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class MethodDocCheckTest extends MagikCheckTestBase {

  @Test
  public void testMethodDoc() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1, param2)\n" +
      "  ## Function  : example\n" +
      "  ##             multi-line\n" +
      "  ## Parameters: PARAM1: example parameter 1\n" +
      "  ##             PARAM2: example parameter 2\n" +
      "  ## Returns   : -\n" +
      "  do_something()\n" +
      "  _if a\n" +
      "  _then\n" +
      "    write(a)\n" +
      "  _endif\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodDocMissing() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1)\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocMissingFunction() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1)\n" +
      "  ## Parameters: PARAM1: example parameters\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocMissingParameters() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1)\n" +
      "  ## Function: example\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocMissingReturns() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1)\n" +
      "  ## Function: example\n" +
      "  ## Parameters: PARAM1: example parameters\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocMissingParametersSingle() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1, param2)\n" +
      "  ## Function: example\n" +
      "  ## Parameters: PARAM1: example parameters\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocMissingParametersOptional() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(param1, _optional param2)\n" +
      "  ## Function: example\n" +
      "  ## Parameters: PARAM1: example parameters\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocParametersAssignment() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b << value\n" +
      "  ## Function: example\n" +
      "  ## Parameters: VALUE: new value\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodDocParametersAssignmentMissing() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b << value\n" +
      "  ## Function: example\n" +
      "  ## Parameters: -\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocParametersIndex() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a[index]\n" +
      "  ## Function: example\n" +
      "  ## Parameters: INDEX: index\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodDocParametersIndexMissing() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a[index]\n" +
      "  ## Function: example\n" +
      "  ## Parameters: -\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMethodDocParametersIndexAssignment() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a[index] << value\n" +
      "  ## Function: example\n" +
      "  ## Parameters: INDEX: index\n" +
      "  ##             VALUE: value\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodDocParametersOptional() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a.b(_optional param1)\n" +
      "  ## Function: example\n" +
      "  ## Parameters: PARAM1: parameter 1\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodDocParametersIndexAssignmentMissing() {
    MagikCheck check = new MethodDocCheck();
    String code =
      "_method a[index] << value\n" +
      "  ## Function: example\n" +
      "  ## Parameters: -\n" +
      "  ## Returns: -\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}