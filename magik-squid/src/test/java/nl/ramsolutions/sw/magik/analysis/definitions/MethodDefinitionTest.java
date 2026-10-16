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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Test {@link MethodDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private MethodDefinition createMethodDefinition(final Instant timestamp) {
    return new MethodDefinition(
        LOCATION,
        timestamp,
        "module",
        null,
        null,
        TYPE_STR_A,
        "method",
        Collections.emptySet(),
        Collections.emptyList(),
        null,
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  @Test
  void testTimestampIgnored() {
    final MethodDefinition methodDef1 = this.createMethodDefinition(Instant.EPOCH);
    final MethodDefinition methodDef2 =
        this.createMethodDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(methodDef1).isEqualTo(methodDef2);
    assertThat(methodDef1.hashCode()).isEqualTo(methodDef2.hashCode());
  }

  private MethodDefinition createNamedMethodDefinition(
      final String methodName,
      final List<ParameterDefinition> parameters,
      final ParameterDefinition assignmentParameter) {
    return new MethodDefinition(
        null,
        null,
        null,
        null,
        null,
        TypeString.ofIdentifier("rope", "sw"),
        methodName,
        Set.of(),
        parameters,
        assignmentParameter,
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  private ParameterDefinition createParameterDefinition(final String name) {
    return new ParameterDefinition(
        null,
        null,
        null,
        null,
        null,
        name,
        ParameterDefinition.Modifier.NONE,
        TypeString.UNDEFINED);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "sw:rope|size|sw:rope.size",
        "sw:rope|add()|sw:rope.add()",
        "sw:rope|size<<|sw:rope.size<<",
        "sw:rope|add()<<|sw:rope.add()<<",
        "sw:rope|int!add()|sw:rope.int!add()",
        "rope|add()|rope.add()",
      })
  void testFormatNameJoinsWithADot(
      final String ownerName, final String methodName, final String expected) {
    final String name = MethodDefinition.formatName(ownerName, methodName);
    assertThat(name).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "sw:rope|[]|sw:rope[]",
        "sw:rope|[]<<|sw:rope[]<<",
        "sw:rope|[]^<<|sw:rope[]^<<",
        "sw:rope|[,]|sw:rope[,]",
        "sw:rope|[index]|sw:rope[index]",
        "rope|[]|rope[]",
      })
  void testFormatNameJoinsAnIndexerWithoutADot(
      final String ownerName, final String methodName, final String expected) {
    final String name = MethodDefinition.formatName(ownerName, methodName);
    assertThat(name).isEqualTo(expected);
  }

  @Test
  void testGetName() {
    final MethodDefinition methodDef = this.createNamedMethodDefinition("add()", List.of(), null);
    final String name = methodDef.getName();
    assertThat(name).isEqualTo("sw:rope.add()");
  }

  @Test
  void testGetNameOfIndexer() {
    final MethodDefinition methodDef = this.createNamedMethodDefinition("[]<<", List.of(), null);
    final String name = methodDef.getName();
    assertThat(name).isEqualTo("sw:rope[]<<");
  }

  @Test
  void testGetNameWithParameters() {
    final ParameterDefinition parameter = this.createParameterDefinition("element");
    final List<ParameterDefinition> parameters = List.of(parameter);
    final MethodDefinition methodDef = this.createNamedMethodDefinition("add()", parameters, null);
    final String name = methodDef.getNameWithParameters();
    assertThat(name).isEqualTo("sw:rope.add(element)");
  }

  @Test
  void testGetNameWithParametersOfIndexer() {
    final ParameterDefinition parameter = this.createParameterDefinition("index");
    final List<ParameterDefinition> parameters = List.of(parameter);
    final ParameterDefinition assignmentParameter = this.createParameterDefinition("value");
    final MethodDefinition methodDef =
        this.createNamedMethodDefinition("[]<<", parameters, assignmentParameter);
    final String name = methodDef.getNameWithParameters();
    assertThat(name).isEqualTo("sw:rope[index]<<value");
  }
}
