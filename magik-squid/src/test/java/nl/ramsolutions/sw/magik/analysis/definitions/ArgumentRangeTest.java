package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link ArgumentRange}. */
class ArgumentRangeTest {

  private ParameterDefinition createParameterDefinition(
      final String name, final ParameterDefinition.Modifier modifier) {
    return new ParameterDefinition(
        null, null, null, null, null, name, modifier, TypeString.UNDEFINED);
  }

  @Test
  void testNoParameters() {
    final List<ParameterDefinition> parameters = List.of();
    final ArgumentRange range = ArgumentRange.of(parameters);
    assertThat(range).isEqualTo(new ArgumentRange(0, 0, false));
  }

  @Test
  void testRequiredParameters() {
    final List<ParameterDefinition> parameters =
        List.of(
            this.createParameterDefinition("a", ParameterDefinition.Modifier.NONE),
            this.createParameterDefinition("b", ParameterDefinition.Modifier.NONE));
    final ArgumentRange range = ArgumentRange.of(parameters);
    assertThat(range).isEqualTo(new ArgumentRange(2, 2, false));
  }

  @Test
  void testOptionalParameters() {
    final List<ParameterDefinition> parameters =
        List.of(
            this.createParameterDefinition("a", ParameterDefinition.Modifier.NONE),
            this.createParameterDefinition("b", ParameterDefinition.Modifier.OPTIONAL),
            this.createParameterDefinition("c", ParameterDefinition.Modifier.OPTIONAL));
    final ArgumentRange range = ArgumentRange.of(parameters);
    assertThat(range).isEqualTo(new ArgumentRange(1, 3, false));
  }

  @Test
  void testGatherParameter() {
    final List<ParameterDefinition> parameters =
        List.of(
            this.createParameterDefinition("a", ParameterDefinition.Modifier.NONE),
            this.createParameterDefinition("b", ParameterDefinition.Modifier.OPTIONAL),
            this.createParameterDefinition("rest", ParameterDefinition.Modifier.GATHER));
    final ArgumentRange range = ArgumentRange.of(parameters);
    assertThat(range).isEqualTo(new ArgumentRange(1, 2, true));
  }

  @Test
  void testOnlyGatherParameter() {
    final List<ParameterDefinition> parameters =
        List.of(this.createParameterDefinition("args", ParameterDefinition.Modifier.GATHER));
    final ArgumentRange range = ArgumentRange.of(parameters);
    assertThat(range).isEqualTo(new ArgumentRange(0, 0, true));
  }
}
