package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link ProcedureDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ProcedureDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private ProcedureDefinition createProcedureDefinition(
      final Instant timestamp, final String moduleName) {
    return new ProcedureDefinition(
        LOCATION,
        timestamp,
        moduleName,
        null,
        null,
        Set.of(),
        TYPE_STR_A,
        "proc",
        Collections.emptyList(),
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  @Test
  void testTimestampIgnored() {
    final ProcedureDefinition procedureDef1 =
        this.createProcedureDefinition(Instant.EPOCH, "module");
    final ProcedureDefinition procedureDef2 =
        this.createProcedureDefinition(Instant.parse("2020-01-01T00:00:00Z"), "module");
    assertThat(procedureDef1).isEqualTo(procedureDef2);
    assertThat(procedureDef1.hashCode()).isEqualTo(procedureDef2.hashCode());
  }

  @Test
  void testModuleNameDistinguishes() {
    final ProcedureDefinition procedureDef1 =
        this.createProcedureDefinition(Instant.EPOCH, "module_a");
    final ProcedureDefinition procedureDef2 =
        this.createProcedureDefinition(Instant.EPOCH, "module_b");
    assertThat(procedureDef1).isNotEqualTo(procedureDef2);
  }

  @Test
  void testGetNameWithParameters() {
    final ParameterDefinition parameter1 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p1",
            ParameterDefinition.Modifier.NONE,
            TypeString.UNDEFINED);
    final ParameterDefinition parameter2 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p2",
            ParameterDefinition.Modifier.OPTIONAL,
            TypeString.UNDEFINED);
    final List<ParameterDefinition> parameters = List.of(parameter1, parameter2);
    final ProcedureDefinition procedureDef =
        new ProcedureDefinition(
            LOCATION,
            Instant.EPOCH,
            "module",
            null,
            null,
            Set.of(),
            TYPE_STR_A,
            "proc",
            parameters,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY);
    final String name = procedureDef.getNameWithParameters();
    assertThat(name).isEqualTo("user:a.invoke(p1, _optional p2)");
  }
}
