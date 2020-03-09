package org.stevenlooman.sw.magik.api;

import com.sonar.sslr.api.Grammar;
import org.junit.Test;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ModuleDefinitionGrammarTest {
  private Grammar g = ModuleDefinitionGrammar.create();

  @Test
  public void testNumber() {
    assertThat(g.rule(ModuleDefinitionGrammar.NUMBER))
        .matches("1");
  }

  @Test
  public void testIdentifier() {
    assertThat(g.rule(ModuleDefinitionGrammar.IDENTIFIER))
        .matches("test_module");
  }

  @Test
  public void testModuleIdentification() {
    assertThat(g.rule(ModuleDefinitionGrammar.MODULE_IDENTIFICATION))
        .matches("test_module 1")
        .matches("test_module 1 1");
  }

  @Test
  public void testConditionMessageAccessor() {
    assertThat(g.rule(ModuleDefinitionGrammar.CONDITION_MESSAGE_ACCESSOR))
        .matches("condition_message_accessor x");
  }

  @Test
  public void testDescription() {
    assertThat(g.rule(ModuleDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(g.rule(ModuleDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  public void testHidden() {
    assertThat(g.rule(ModuleDefinitionGrammar.HIDDEN))
        .matches("hidden");
  }

  @Test
  public void testLanguage() {
    assertThat(g.rule(ModuleDefinitionGrammar.LANGUAGE))
        .matches("language en_gb");
  }

  @Test
  public void testMessages() {
    assertThat(g.rule(ModuleDefinitionGrammar.MESSAGES))
        .matches("messages a")
        .matches("messages a b c");
  }

  @Test
  public void testOptional() {
    assertThat(g.rule(ModuleDefinitionGrammar.OPTIONAL))
        .matches("optional\nend")
        .matches("optional\nx\nend")
        .matches("optional\nx 1\nend")
        .matches("optional\nx 1\ny 2\nend");
  }

  @Test
  public void testRequires() {
    assertThat(g.rule(ModuleDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  public void testRequiresDatamodel() {
    assertThat(g.rule(ModuleDefinitionGrammar.REQUIRES_DATAMODEL))
        .matches("requires_datamodel\nend")
        .matches("requires_datamodel\ndb\nend")
        .matches("requires_datamodel\ndb1 gis\nend")
        .matches("requires_datamodel\ndb1 gis elec\nend")
        .matches("requires_datamodel\ndb1 gis elec 1\nend")
        .matches("requires_datamodel\ndb1 gis elec 1 1\nend")
        .matches("requires_datamodel\ndb1 gis elec 1 1\ndb2 schema elec 1 1\nend");
  }

  @Test
  public void testTemplates() {
    assertThat(g.rule(ModuleDefinitionGrammar.TEMPLATES))
        .matches("templates x")
        .matches("templates x y z");
  }

  @Test
  public void testTest() {
    assertThat(g.rule(ModuleDefinitionGrammar.TEST))
        .matches("test\nend")
        .matches("test\nname x_tests\nend")
        .matches("test\nframework munit\nend")
        .matches("test\ntopics a\nend")
        .matches("test\ntopics a,b,c\nend")
        .matches("test\nargs a 1 b 2\nend")
        .matches("test\ndescription etc etc etc\nend")
        .matches("test\nlabel l1\nend")
        .matches("test\ntopic t1\nend")
        .matches("test\narg a1\nend")
        .matches("test\nname x_tests\nframework munit\nend");
  }

  @Test
  public void testTestsModules() {
    assertThat(g.rule(ModuleDefinitionGrammar.TESTS_MODULES))
        .matches("tests_modules\nend")
        .matches("tests_modules\nx\nend")
        .matches("tests_modules\nx 1\nend")
        .matches("tests_modules\nx 1\ny 2\nend");
  }

  @Test
  public void testAceInstallation() {
    assertThat(g.rule(ModuleDefinitionGrammar.ACE_INSTALLATION))
        .matches("ace_installation\nend")
        .matches("ace_installation\nx\nend")
        .matches("ace_installation\nx\ny\nz\nend")
        .matches("ace_installation\nx y z\nend");
  }

  @Test
  public void testAuthInstallation() {
    assertThat(g.rule(ModuleDefinitionGrammar.AUTH_INSTALLATION))
        .matches("auth_installation\nend")
        .matches("auth_installation\nx\nend")
        .matches("auth_installation\nx\ny\nz\nend")
        .matches("auth_installation\nx y z\nend");
  }

  @Test
  public void testCaseInstallation() {
    assertThat(g.rule(ModuleDefinitionGrammar.CASE_INSTALLATION))
        .matches("case_installation\nend")
        .matches("case_installation\nx\nend")
        .matches("case_installation\nx\ny\nz\nend")
        .matches("case_installation\nx y z\nend");
  }

  @Test
  public void testStyleInstallation() {
    assertThat(g.rule(ModuleDefinitionGrammar.STYLE_INSTALLATION))
        .matches("style_installation\nend")
        .matches("style_installation\nx\nend")
        .matches("style_installation\nx\ny\nz\nend")
        .matches("style_installation\nx y z\nend");
  }

  @Test
  public void testSystemInstallation() {
    assertThat(g.rule(ModuleDefinitionGrammar.SYSTEM_INSTALLATION))
        .matches("system_installation\nend")
        .matches("system_installation\nx\nend")
        .matches("system_installation\nx\ny\nz\nend")
        .matches("system_installation\nx y z\nend");
  }
}
