package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class VariableNamingCheckTest extends MagikCheckTest {

  @Test
  public void testValidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local coord";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testInvalidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local c";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testWhitelistedName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local x";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testValidNameParameter() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_method a.b(coord) _endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testInvalidNameParameter() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_method a.b(c) _endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testPrefixedValidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local l_coord";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testPrefixedInvalidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local l_c";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testWhitelistedPrefixedName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local l_x";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMultiVariableDeclarationValidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local (l_item, l_result) << (1, 2)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMultiVariableDeclarationInvalidName() {
    MagikCheck check = new VariableNamingCheck();
    String code =
        "_local (l_i, l_r) << (1, 2)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
