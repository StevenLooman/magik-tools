package nl.ramsolutions.sw.magik;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Tests for {@link SourcePathResolver}. */
class SourcePathResolverTest {

  @Test
  void testUriWithoutVariableIsReturnedUnchanged() {
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", "/opt/sw530")::get;
    final URI uri = URI.create("file:///home/me/proj/foo.magik");
    assertThat(SourcePathResolver.expand(uri, env)).isEqualTo(uri);
  }

  @Test
  void testVariableIsExpandedToRealPathOnAnyOs() {
    final Path gisRoot = Path.of(System.getProperty("java.io.tmpdir"), "sw530");
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", gisRoot.toString())::get;
    final URI uri = URI.create("file:///$SMALLWORLD_GIS/sw_core/foo.magik");

    final URI expanded = SourcePathResolver.expand(uri, env);

    assertThat(Path.of(expanded)).isEqualTo(gisRoot.resolve("sw_core").resolve("foo.magik"));
  }

  @Test
  void testUnresolvedVariableIsLeftLiteral() {
    final URI uri = URI.create("file:///$NOT_SET/foo.magik");
    assertThat(SourcePathResolver.expand(uri, name -> null)).isEqualTo(uri);
  }

  @Test
  void testPrefixMappingRewritesRawAbsolutePath() {
    final Path localRoot = Path.of(System.getProperty("java.io.tmpdir"), "gma");
    final URI uri = URI.create("file:///C:/projects/hg/gma/foo.magik");
    final Map<String, String> mappings = Map.of("C:/projects/hg/gma", localRoot.toString());

    final URI expanded = SourcePathResolver.expand(uri, name -> null, mappings);

    assertThat(Path.of(expanded)).isEqualTo(localRoot.resolve("foo.magik"));
  }

  @Test
  void testPrefixMappingChainsIntoVariableExpansion() {
    final Path gisRoot = Path.of(System.getProperty("java.io.tmpdir"), "sw530");
    final URI uri = URI.create("file:///C:/projects/hg/gma/foo.magik");
    final Map<String, String> mappings = Map.of("C:/projects/hg/gma", "$SMALLWORLD_GIS/gma");
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", gisRoot.toString())::get;

    final URI expanded = SourcePathResolver.expand(uri, env, mappings);

    assertThat(Path.of(expanded)).isEqualTo(gisRoot.resolve("gma").resolve("foo.magik"));
  }

  @Test
  void testLongestPrefixMappingWins() {
    final Path a = Path.of(System.getProperty("java.io.tmpdir"), "a");
    final Path b = Path.of(System.getProperty("java.io.tmpdir"), "b");
    final URI uri = URI.create("file:///C:/projects/hg/gma/foo.magik");
    final Map<String, String> mappings =
        Map.of("C:/projects", a.toString(), "C:/projects/hg/gma", b.toString());

    final URI expanded = SourcePathResolver.expand(uri, name -> null, mappings);

    assertThat(Path.of(expanded)).isEqualTo(b.resolve("foo.magik"));
  }

  @Test
  void testNoPrefixMappingLeavesUriUnchanged() {
    final URI uri = URI.create("file:///home/me/proj/foo.magik");
    assertThat(SourcePathResolver.expand(uri, name -> null, Map.of())).isEqualTo(uri);
  }
}
