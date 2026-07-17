package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link ExemplarDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ExemplarDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private ExemplarDefinition createExemplarDefinition(final Instant timestamp) {
    return new ExemplarDefinition(
        LOCATION,
        timestamp,
        "module",
        null,
        null,
        ExemplarDefinition.Sort.SLOTTED,
        TYPE_STR_A,
        List.of(TypeString.SW_OBJECT),
        null);
  }

  @Test
  void testTimestampIgnored() {
    final ExemplarDefinition exemplarDef1 = this.createExemplarDefinition(Instant.EPOCH);
    final ExemplarDefinition exemplarDef2 =
        this.createExemplarDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(exemplarDef1).isEqualTo(exemplarDef2);
    assertThat(exemplarDef1.hashCode()).isEqualTo(exemplarDef2.hashCode());
  }
}
