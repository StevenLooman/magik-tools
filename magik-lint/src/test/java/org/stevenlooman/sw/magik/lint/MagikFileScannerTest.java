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

public class MagikFileScannerTest {

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
  public void testScanFiles() throws IOException {
    Path testProductPath = getPath("magik-lint/src/test/resources/test_product");
    Collection<Path> actualPathsCollection = MagikFileScanner.scanMagikFiles(testProductPath);
    Set<Path> actualPaths = new HashSet<>(actualPathsCollection);

    Set<Path> expectedPaths = new HashSet<>(Arrays.asList(
      getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source/b.magik"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source/sw/integer.magik")
    ));
    assertEquals(expectedPaths, actualPaths);
  }

 @Test
 public void testgetFilesFromArgsSingleFile() throws IOException {
   Path path = getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik");
   String[] args = new String[] {path.toString()};
   Collection<Path> actual = MagikFileScanner.getFilesFromArgs(args);
   Set<Path> actualPaths = new HashSet<>(actual);

   Set<Path> expectedPaths = new HashSet<>(Arrays.asList(
    getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik")
    ));
    assertEquals(expectedPaths, actualPaths);
  }
  
  @Test
  public void testgetFilesFromArgsDirectory() throws IOException {
    Path path = getPath("magik-lint/src/test/resources/test_product/test_module/source");
    String[] args = new String[] {path.toString()};
    Collection<Path> actual = MagikFileScanner.getFilesFromArgs(args);
    Set<Path> actualPaths = new HashSet<>(actual);
 
    Set<Path> expectedPaths = new HashSet<>(Arrays.asList(
      getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source/b.magik"),
      getPath("magik-lint/src/test/resources/test_product/test_module/source/sw/integer.magik")
    ));
    assertEquals(expectedPaths, actualPaths);
   }

 }