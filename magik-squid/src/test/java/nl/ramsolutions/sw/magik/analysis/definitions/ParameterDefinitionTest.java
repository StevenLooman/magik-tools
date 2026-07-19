package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link ParameterDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ParameterDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private ParameterDefinition createParameterDefinition(
      final Instant timestamp, final String moduleName) {
    return new ParameterDefinition(
        LOCATION,
        timestamp,
        moduleName,
        null,
        null,
        "p",
        ParameterDefinition.Modifier.NONE,
        TYPE_STR_A);
  }

  @Test
  void testTimestampIgnored() {
    final ParameterDefinition parameterDef1 =
        this.createParameterDefinition(Instant.EPOCH, "module");
    final ParameterDefinition parameterDef2 =
        this.createParameterDefinition(Instant.parse("2020-01-01T00:00:00Z"), "module");
    assertThat(parameterDef1).isEqualTo(parameterDef2);
    assertThat(parameterDef1.hashCode()).isEqualTo(parameterDef2.hashCode());
  }

  @Test
  void testModuleNameDistinguishes() {
    final ParameterDefinition parameterDef1 =
        this.createParameterDefinition(Instant.EPOCH, "module_a");
    final ParameterDefinition parameterDef2 =
        this.createParameterDefinition(Instant.EPOCH, "module_b");
    assertThat(parameterDef1).isNotEqualTo(parameterDef2);
  }
}
