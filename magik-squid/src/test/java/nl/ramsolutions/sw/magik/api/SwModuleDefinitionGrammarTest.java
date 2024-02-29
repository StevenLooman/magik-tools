package nl.ramsolutions.sw.magik.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import nl.ramsolutions.sw.definitions.api.SwModuleDefinitionGrammar;
import org.junit.jupiter.api.Test;

/** Tests for SwModuleDefinitionGrammar. */
class SwModuleDefinitionGrammarTest {
  private Grammar g = SwModuleDefinitionGrammar.create();

  @Test
  void testNumber() {
    assertThat(g.rule(SwModuleDefinitionGrammar.NUMBER)).matches("1");
  }

  @Test
  void testIdentifier() {
    assertThat(g.rule(SwModuleDefinitionGrammar.IDENTIFIER)).matches("test_module");
  }

  @Test
  void testModuleIdentification() {
    assertThat(g.rule(SwModuleDefinitionGrammar.MODULE_IDENTIFICATION))
        .matches("test_module 1")
        .matches("test_module 1 1");
  }

  @Test
  void testConditionMessageAccessor() {
    assertThat(g.rule(SwModuleDefinitionGrammar.CONDITION_MESSAGE_ACCESSOR))
        .matches("condition_message_accessor x");
  }

  @Test
  void testDescription() {
    assertThat(g.rule(SwModuleDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(g.rule(SwModuleDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  void testHidden() {
    assertThat(g.rule(SwModuleDefinitionGrammar.HIDDEN)).matches("hidden");
  }

  @Test
  void testLanguage() {
    assertThat(g.rule(SwModuleDefinitionGrammar.LANGUAGE)).matches("language en_gb");
  }

  @Test
  void testMessages() {
    assertThat(g.rule(SwModuleDefinitionGrammar.MESSAGES))
        .matches("messages a")
        .matches("messages a b c");
  }

  @Test
  void testOptional() {
    assertThat(g.rule(SwModuleDefinitionGrammar.OPTIONAL))
        .matches("optional\nend")
        .matches("optional\nx\nend")
        .matches("optional\nx 1\nend")
        .matches("optional\nx 1\ny 2\nend");
  }

  @Test
  void testRequires() {
    assertThat(g.rule(SwModuleDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  void testRequiresJava() {
    assertThat(g.rule(SwModuleDefinitionGrammar.REQUIRES_JAVA))
        .matches("requires_java\n\tcom.gesmallworld.magik.http\nend");
  }

  @Test
  void testRequiresDatamodel() {
    assertThat(g.rule(SwModuleDefinitionGrammar.REQUIRES_DATAMODEL))
        .matches("requires_datamodel\nend")
        .matches("requires_datamodel\ndb\nend")
        .matches("requires_datamodel\ndb1 gis\nend")
        .matches("requires_datamodel\ndb1 gis elec\nend")
        .matches("requires_datamodel\ndb1 gis elec 1\nend")
        .matches("requires_datamodel\ndb1 gis elec 1 1\nend")
        .matches("requires_datamodel\ndb1 gis elec 1 1\ndb2 schema elec 1 1\nend");
  }

  @Test
  void testTemplates() {
    assertThat(g.rule(SwModuleDefinitionGrammar.TEMPLATES))
        .matches("templates\nend")
        .matches("templates\ndb\nend");
  }

  @Test
  void testTest() {
    assertThat(g.rule(SwModuleDefinitionGrammar.TEST))
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
  void testTestsModules() {
    assertThat(g.rule(SwModuleDefinitionGrammar.TESTS_MODULES))
        .matches("tests_modules\nend")
        .matches("tests_modules\nx\nend")
        .matches("tests_modules\nx 1\nend")
        .matches("tests_modules\nx 1\ny 2\nend");
  }

  @Test
  void testAceInstallation() {
    assertThat(g.rule(SwModuleDefinitionGrammar.ACE_INSTALLATION))
        .matches("ace_installation\nend")
        .matches("ace_installation\nx\nend")
        .matches("ace_installation\nx\ny\nz\nend")
        .matches("ace_installation\nx y z\nend");
  }

  @Test
  void testAuthInstallation() {
    assertThat(g.rule(SwModuleDefinitionGrammar.AUTH_INSTALLATION))
        .matches("auth_installation\nend")
        .matches("auth_installation\nx\nend")
        .matches("auth_installation\nx\ny\nz\nend")
        .matches("auth_installation\nx y z\nend");
  }

  @Test
  void testCaseInstallation() {
    assertThat(g.rule(SwModuleDefinitionGrammar.CASE_INSTALLATION))
        .matches("case_installation\nend")
        .matches("case_installation\nx\nend")
        .matches("case_installation\nx\ny\nz\nend")
        .matches("case_installation\nx y z\nend");
  }

  @Test
  void testStyleInstallation() {
    assertThat(g.rule(SwModuleDefinitionGrammar.STYLE_INSTALLATION))
        .matches("style_installation\nend")
        .matches("style_installation\nx\nend")
        .matches("style_installation\nx\ny\nz\nend")
        .matches("style_installation\nx y z\nend");
  }

  @Test
  void testSystemInstallation() {
    assertThat(g.rule(SwModuleDefinitionGrammar.SYSTEM_INSTALLATION))
        .matches("system_installation\nend")
        .matches("system_installation\nx\nend")
        .matches("system_installation\nx\ny\nz\nend")
        .matches("system_installation\nx y z\nend");
  }
}
