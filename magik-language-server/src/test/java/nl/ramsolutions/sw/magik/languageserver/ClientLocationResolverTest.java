package nl.ramsolutions.sw.magik.languageserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.SourcePathResolver;
import org.junit.jupiter.api.Test;

class ClientLocationResolverTest {

  @Test
  void testCollapsesEnvVarAndConcreteThatExpandToSameFileKeepingRanged() {
    final Path gisRoot = Path.of(System.getProperty("java.io.tmpdir"), "sw530");
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", gisRoot.toString())::get;
    final URI concreteUri = gisRoot.resolve("core").resolve("foo.magik").toUri();

    final Location classInfo =
        new Location(URI.create("file:///$SMALLWORLD_GIS/core/foo.magik")); // range-less
    final Location indexed =
        new Location(concreteUri, new Range(new Position(3, 0), new Position(3, 5)));

    final List<Location> result =
        ClientLocationResolver.resolveAndDedup(
            List.of(classInfo, indexed), new SourcePathResolver(env, Map.of()));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUri()).isEqualTo(concreteUri);
    assertThat(result.get(0).getRange()).isNotNull();
  }

  @Test
  void testKeepsTwoThatExpandToDifferentFiles() {
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", "/opt/sw530")::get;
    final Location a = new Location(URI.create("file:///proj/a.magik"));
    final Location b = new Location(URI.create("file:///proj/b.magik"));

    final List<Location> result =
        ClientLocationResolver.resolveAndDedup(
            List.of(a, b), new SourcePathResolver(env, Map.of()));

    assertThat(result).hasSize(2);
  }

  @Test
  void testKeepsMultipleRangedHitsInTheSameFile() {
    // references returns many hits in one file at different ranges; dedup must not merge them.
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", "/opt/sw530")::get;
    final URI uri = URI.create("file:///proj/a.magik");
    final Location hit1 = new Location(uri, new Range(new Position(1, 0), new Position(1, 3)));
    final Location hit2 = new Location(uri, new Range(new Position(7, 0), new Position(7, 3)));

    final List<Location> result =
        ClientLocationResolver.resolveAndDedup(
            List.of(hit1, hit2), new SourcePathResolver(env, Map.of()));

    assertThat(result).containsExactly(hit1, hit2);
  }
}
