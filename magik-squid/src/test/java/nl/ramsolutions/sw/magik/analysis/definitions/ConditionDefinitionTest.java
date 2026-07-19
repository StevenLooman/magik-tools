package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import org.junit.jupiter.api.Test;

/** Test {@link ConditionDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ConditionDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));

  private ConditionDefinition createConditionDefinition(final Instant timestamp) {
    return new ConditionDefinition(
        LOCATION, timestamp, "module", null, null, "cond", "parent", Collections.emptyList(), null);
  }

  @Test
  void testTimestampIgnored() {
    final ConditionDefinition conditionDef1 = this.createConditionDefinition(Instant.EPOCH);
    final ConditionDefinition conditionDef2 =
        this.createConditionDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(conditionDef1).isEqualTo(conditionDef2);
    assertThat(conditionDef1.hashCode()).isEqualTo(conditionDef2.hashCode());
  }
}
