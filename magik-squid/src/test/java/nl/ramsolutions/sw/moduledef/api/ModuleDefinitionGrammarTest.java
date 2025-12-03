package nl.ramsolutions.sw.moduledef.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleDefinitionGrammar}. */
class ModuleDefinitionGrammarTest {
  private final Grammar grammar = ModuleDefinitionGrammar.create();

  @Test
  void testNumber() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.NUMBER)).matches("1");
  }

  @Test
  void testIdentifier() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.IDENTIFIER)).matches("test_module");
  }

  @Test
  void testModuleIdentification() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.MODULE_IDENTIFICATION))
        .matches("test_module 1")
        .matches("test_module 1 1");
  }

  @Test
  void testConditionMessageAccessor() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.CONDITION_MESSAGE_ACCESSOR))
        .matches("condition_message_accessor x");
  }

  @Test
  void testDescription() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(grammar.rule(ModuleDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  void testDoNotTranslate() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.DO_NOT_TRANSLATE)).matches("do_not_translate");
  }

  @Test
  void testHidden() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.HIDDEN)).matches("hidden");
  }

  @Test
  void testLanguage() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.LANGUAGE)).matches("language en_gb");
  }

  @Test
  void testMessages() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.MESSAGES))
        .matches("messages a")
        .matches("messages a b c");
  }

  @Test
  void testOptional() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.OPTIONAL))
        .matches("optional\nend")
        .matches("optional\nx\nend")
        .matches("optional\nx 1\nend")
        .matches("optional\nx 1\ny 2\nend");
  }

  @Test
  void testRequiredBy() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.REQUIRED_BY))
        .matches("required_by\nend")
        .matches("required_by\nx\nend")
        .matches("required_by\nx 1\nend")
        .matches("required_by\nx 1\ny 2\nend");
  }

  @Test
  void testRequires() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  void testRequiresJava() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.REQUIRES_JAVA))
        .matches("requires_java\n\tcom.gesmallworld.magik.http\nend");
  }

  @Test
  void testRequiresDatamodel() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.REQUIRES_DATAMODEL))
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
    assertThat(grammar.rule(ModuleDefinitionGrammar.TEMPLATES))
        .matches("templates\nend")
        .matches("templates\ndb\nend");
  }

  @Test
  void testTest() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.TEST))
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
    assertThat(grammar.rule(ModuleDefinitionGrammar.TESTS_MODULES))
        .matches("tests_modules\nend")
        .matches("tests_modules\nx\nend")
        .matches("tests_modules\nx 1\nend")
        .matches("tests_modules\nx 1\ny 2\nend");
  }

  @Test
  void testAceInstallation() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.ACE_INSTALLATION))
        .matches("ace_installation\nend")
        .matches("ace_installation\nx\nend")
        .matches("ace_installation\nx\ny\nz\nend")
        .matches("ace_installation\nx y z\nend");
  }

  @Test
  void testAuthInstallation() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.AUTH_INSTALLATION))
        .matches("auth_installation\nend")
        .matches("auth_installation\nx\nend")
        .matches("auth_installation\nx\ny\nz\nend")
        .matches("auth_installation\nx y z\nend");
  }

  @Test
  void testCaseInstallation() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.CASE_INSTALLATION))
        .matches("case_installation\nend")
        .matches("case_installation\nx\nend")
        .matches("case_installation\nx\ny\nz\nend")
        .matches("case_installation\nx y z\nend");
  }

  @Test
  void testStyleInstallation() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.STYLE_INSTALLATION))
        .matches("style_installation\nend")
        .matches("style_installation\nx\nend")
        .matches("style_installation\nx\ny\nz\nend")
        .matches("style_installation\nx y z\nend");
  }

  @Test
  void testSystemInstallation() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.SYSTEM_INSTALLATION))
        .matches("system_installation\nend")
        .matches("system_installation\nx\nend")
        .matches("system_installation\nx\ny\nz\nend")
        .matches("system_installation\nx y z\nend");
  }

  @Test
  void testSyntaxError() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.SYNTAX_ERROR_SECTION)).matches("abc\nend");
  }

  @Test
  void testMethodDefinition() {
    assertThat(grammar.rule(ModuleDefinitionGrammar.MODULE_DEFINITION))
        .matches("")
        .matches("module")
        .matches("module 1 2 3 4 5")
        .matches("module x")
        .matches("module 1")
        .matches(
            """
            test_module_a 1
            requires
              test_module_b
            end
            """)
        .matches(
            """
            test_module_a 1
            reqs
              test_module_b
            end
            """) // Syntax error.
        .matches(
            """
            test_module_a 1
            some extra line""") // Syntax error.
        .matches(
            """
            test_module_a 1
            some extra line
            """) // Syntax error.
        .matches(
            """
            test_module_a 1
            requires
              test_module_b
            end

            end
            """); // Syntax error.
  }
}
