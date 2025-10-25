package nl.ramsolutions.sw.checks.moduledef;

import static nl.ramsolutions.sw.checks.moduledef.ModuleDefCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.ModuleDefCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ModuleDefRequiredModuleAlreadyInTestsModulesCheck}. */
class ModuleDefRequiredModuleAlreadyInTestsModulesCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        example_module_test 1

        requires
          other_module
          another_module
        end

        tests_modules
          example_module
        end
        """,
        """
        example_module_test 1

        tests_modules
          example_module
        end
        """,
        """
        example_module_test 1

        requires
        end

        tests_modules
          example_module
        end
        """,
        """
        example_module_test 1

        requires
          other_module
          another_module
        end

        tests_modules

        end
        """,
        """
        example_module_test 1

        requires
          other_module
          another_module
          example_module
        end

        tests_modules

        end
        """,
      })
  void testOk(final String code) {
    final ModuleDefCheck check = new ModuleDefRequiredModuleAlreadyInTestsModulesCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testInvalid() {
    final ModuleDefCheck check = new ModuleDefRequiredModuleAlreadyInTestsModulesCheck();
    final String code =
        """
        example_module_tests 1

        requires
            example_module
        end

        tests_modules
            example_module
        end
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
