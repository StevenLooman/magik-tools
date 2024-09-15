package nl.ramsolutions.sw.productdef;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;

/** Product.def file scanner. */
public final class ProductDefFileScanner {

  /** Product.def tree. */
  public static final class Tree {

    private final Tree parent;
    private final Path path;
    private final Set<Tree> children = new HashSet<>();

    public Tree(@Nullable final Tree parent, final Path path) {
      this.parent = parent;
      this.path = path;
    }

    @CheckForNull
    public Tree getParent() {
      return this.parent;
    }

    public Path getPath() {
      return this.path;
    }

    public Set<Tree> getChildren() {
      return Collections.unmodifiableSet(this.children);
    }

    private void addChild(final Tree child) {
      this.children.add(child);
    }

    /**
     * Get a stream of self and children.
     *
     * @return Stream of self and children.
     */
    public Stream<Tree> stream() {
      return Stream.concat(Stream.of(this), this.children.stream().flatMap(Tree::stream));
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (this.getClass() != obj.getClass()) {
        return false;
      }

      final Tree other = (Tree) obj;
      return Objects.equals(this.path, other.path);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.path);
    }
  }

  /** Product definition file visitor. */
  private static final class ProductDefFileVisitor extends SimpleFileVisitor<Path> {

    private final Deque<Tree> stack = new ArrayDeque<>();
    private final IgnoreHandler ignoreHandler;
    private Tree root = null;

    public ProductDefFileVisitor(final IgnoreHandler ignoreHandler) {
      this.ignoreHandler = ignoreHandler;
    }

    @CheckForNull
    public Tree getProductDefTree() {
      return this.root;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {
      final Tree parentProductDefTree = this.stack.peek();

      // If ignored, skip from there.
      final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
      if (this.ignoreHandler.isIgnored(productDefPath)) {
        return FileVisitResult.SKIP_SUBTREE;
      }

      // If product.def exists, add it to the tree.
      final Tree currentProductDefTree;
      if (Files.exists(productDefPath)) {
        final Tree newProductDefTree = new Tree(parentProductDefTree, productDefPath);
        parentProductDefTree.addChild(newProductDefTree);
        currentProductDefTree = newProductDefTree;
      } else {
        currentProductDefTree = parentProductDefTree;
      }

      this.stack.push(currentProductDefTree);

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
        throws IOException {
      this.stack.pop();

      return FileVisitResult.CONTINUE;
    }
  }

  public static final String SW_PRODUCT_DEF = "product.def";

  private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefFileScanner.SW_PRODUCT_DEF);
  private final IgnoreHandler ignoreHandler;

  public ProductDefFileScanner(final IgnoreHandler ignoreHandler) {
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Get the magik files from the given path.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of paths to magik files.
   * @throws IOException -
   */
  @CheckForNull
  public Tree getProductTree(final Path fromPath) throws IOException {
    final ProductDefFileVisitor visitor = new ProductDefFileVisitor(this.ignoreHandler);
    Files.walkFileTree(fromPath, visitor);
    return visitor.getProductDefTree();
  }

  /**
   * Get the product definition path for the given module.
   *
   * @param searchPath Path to search from.
   * @return Path to the product definition file.
   * @throws IOException -
   */
  @CheckForNull
  public Path getProductDefPathForModule(final Path searchPath) throws IOException {
    final Path productDefPath = searchPath.resolve(ModuleDefFileScanner.SW_PRODUCT_DEF);
    if (Files.exists(productDefPath)) {
      return productDefPath;
    }

    final Path parentPath = searchPath.getParent();
    if (parentPath == null) {
      return null;
    }

    // Recurse upwards.
    return this.getProductDefPathForModule(parentPath);
  }
}
