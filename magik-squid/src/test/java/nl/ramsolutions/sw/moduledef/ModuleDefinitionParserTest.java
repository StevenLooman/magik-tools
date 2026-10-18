package nl.ramsolutions.sw.moduledef;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleDefinitionParser}. */
class ModuleDefinitionParserTest {

  private static final URI MODULE_DEF_URI = URI.create("memory://module.def");

  private ModuleDefinition parse(final String code) {
    final ModuleDefFile moduleDefFile =
        new ModuleDefFile(MODULE_DEF_URI, code, new DefinitionKeeper(false), null);
    return moduleDefFile.getModuleDefinition();
  }

  @Test
  void testLanguageAndConditionMessageAccessor() {
    final String code =
        """
        test_module 1

        description
        \tTest module
        end

        language nl_nl
        condition_message_accessor test_conditions
        """;
    final ModuleDefinition definition = this.parse(code);

    assertThat(definition.getLanguage()).isEqualTo("nl_nl");
    assertThat(definition.getConditionMessageAccessor()).isEqualTo("test_conditions");
  }

  @Test
  void testWithoutLanguageAndConditionMessageAccessor() {
    final String code =
        """
        test_module 1
        """;
    final ModuleDefinition definition = this.parse(code);

    assertThat(definition.getLanguage()).isNull();
    assertThat(definition.getConditionMessageAccessor()).isNull();
  }
}
