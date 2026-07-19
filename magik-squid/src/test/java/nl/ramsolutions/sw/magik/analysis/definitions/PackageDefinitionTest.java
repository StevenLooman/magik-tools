package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import org.junit.jupiter.api.Test;

/** Test {@link PackageDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class PackageDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));

  private PackageDefinition createPackageDefinition(
      final Instant timestamp, final String moduleName) {
    return new PackageDefinition(
        LOCATION, timestamp, moduleName, null, null, "pkg", Collections.emptyList());
  }

  @Test
  void testTimestampIgnored() {
    final PackageDefinition packageDef1 = this.createPackageDefinition(Instant.EPOCH, "module");
    final PackageDefinition packageDef2 =
        this.createPackageDefinition(Instant.parse("2020-01-01T00:00:00Z"), "module");
    assertThat(packageDef1).isEqualTo(packageDef2);
    assertThat(packageDef1.hashCode()).isEqualTo(packageDef2.hashCode());
  }

  @Test
  void testModuleNameDistinguishes() {
    final PackageDefinition packageDef1 = this.createPackageDefinition(Instant.EPOCH, "module_a");
    final PackageDefinition packageDef2 = this.createPackageDefinition(Instant.EPOCH, "module_b");
    assertThat(packageDef1).isNotEqualTo(packageDef2);
  }
}
