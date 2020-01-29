package org.stevenlooman.sw.magik.lint;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class MagikFileWatcherTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   * 
   * @return Proper {{Path}} to use.
   */
  private Path getPath(String relativePath) {
    Path path = Paths.get(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-lint")) {
      return Paths.get("..").resolve(relativePath);
    }
    return Paths.get(".").resolve(relativePath);
  }

  @Test
  public void testScanDirectories() throws IOException {
    Path testProductPath = getPath("magik-lint/src/test/resources/test_product");
    Collection<Path> actualPathsCollection = MagikFileWatcher.scanDirectories(testProductPath);
    Set<Path> actualPaths = new HashSet<>(actualPathsCollection);

    Set<Path> expectedPaths = new HashSet<>(Arrays.asList(
      getPath("magik-lint/src/test/resources/test_product"),
      getPath("magik-lint/src/test/resources/test_product/test_module"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source/sw")
    ));
    assertEquals(expectedPaths, actualPaths);
 }

}