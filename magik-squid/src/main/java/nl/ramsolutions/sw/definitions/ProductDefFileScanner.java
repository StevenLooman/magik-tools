package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.magik.ProductDefFile;

/** product.def file scanner. */
public final class ProductDefFileScanner {

  private static final class ProductDefFileVisitor extends SimpleFileVisitor<Path> {

    private final Deque<ProductDefFile> stack = new ArrayDeque<>();
    private final Set<ProductDefFile> productDefFiles = new HashSet<>();

    public Set<ProductDefFile> getProductDefFiles() {
      return Collections.unmodifiableSet(productDefFiles);
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {
      final ProductDefFile parentProductDefFile = this.stack.peek();

      final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
      final ProductDefFile currentProductDefFile =
          Files.exists(productDefPath) ? this.addProduct(productDefPath) : parentProductDefFile;
      this.stack.push(currentProductDefFile);

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
        throws IOException {
      this.stack.pop();

      return FileVisitResult.CONTINUE;
    }

    private ProductDefFile addProduct(final Path path) {
      final ProductDefFile parentProduct = this.stack.peek();

      try {
        // TODO: DefinitionKeeper
        final ProductDefFile currentProductDefFile = new ProductDefFile(path, null, parentProduct);
        this.productDefFiles.add(currentProductDefFile);

        return currentProductDefFile;
      } catch (final IOException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  /** Product definition filename. */
  public static final String SW_PRODUCT_DEF = "product.def";

  private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefFileScanner.SW_PRODUCT_DEF);

  private ProductDefFileScanner() {}

  /**
   * Scan for products.
   *
   * @param path Path to scan from.
   * @return All found {@link ProductDefFile}s.
   * @throws IOException -
   */
  public static Set<ProductDefFile> scanProductFiles(final Path path) throws IOException {
    final ProductDefFileVisitor fileVisitor = new ProductDefFileVisitor();
    Files.walkFileTree(path, fileVisitor);
    return fileVisitor.getProductDefFiles();
  }

  /**
   * Get product from a given path, iterates upwards to find product.def file.
   *
   * <p>Note that this does *NOT* set the parent product.
   *
   * @param startPath Path to start at.
   * @return Parsed product definition.
   * @throws IOException -
   */
  @CheckForNull
  public static ProductDefFile getProductDefFileForPath(final Path startPath) throws IOException {
    Path path = startPath;
    while (path != null) {
      final Path productDefPath = path.resolve(SW_PRODUCT_DEF_PATH);
      final File productDefFile = productDefPath.toFile();
      if (productDefFile.exists()) {
        return new ProductDefFile(productDefPath, null, null);
      }

      path = path.getParent();
    }

    return null;
  }
}
