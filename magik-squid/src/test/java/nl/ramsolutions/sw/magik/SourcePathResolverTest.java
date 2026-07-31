package nl.ramsolutions.sw.magik;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SourcePathResolverTest {

  @Test
  void testUriWithoutVariableIsReturnedUnchanged() {
    final Function<String, String> env = Map.of("SMALLWORLD_GIS", "/opt/sw530")::get;
    final URI uri = URI.create("file:///home/me/proj/foo.magik");
    assertThat(SourcePathResolver.expand(uri, env)).isEqualTo(uri);
  }

  @Test
  void testVariableIsExpandedToRealPathOnAnyOs() {
    // Use a real OS path as the logical's value so the assertion holds on POSIX and Windows alike.
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
}
