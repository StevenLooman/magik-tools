package nl.ramsolutions.sw.checks.loadlist;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.LoadListCheck;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import org.sonar.check.Rule;

/** Check that entries in a load_list.txt/patch_list.txt file actually exist. */
@Rule(key = LoadListEntryExistsCheck.CHECK_KEY)
public class LoadListEntryExistsCheck extends LoadListCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "LoadListEntryExists";

  private static final String MESSAGE_FILE = "File does not exist: %s";
  private static final String MESSAGE_DIRECTORY = "Directory does not exist: %s";
  private static final String MESSAGE_NESTED_LOAD_LIST =
      "Directory exists but does not contain a load_list.txt: %s";

  @Override
  protected void scanFile() {
    final LoadListFile loadListFile = this.getLoadListFile();
    final URI uri = loadListFile.getUri();
    if (!uri.getScheme().equals("file")) {
      // Can only check file URIs.
      return;
    }

    final Path loadListPath = Path.of(uri);
    final Path parentDir = loadListPath.getParent();
    if (parentDir == null) {
      return;
    }

    final AstNode topNode = loadListFile.getTopNode();
    topNode
        .getDescendants(LoadListGrammar.FILE_PATH)
        .forEach(node -> this.checkEntry(node, parentDir));
  }

  private void checkEntry(final AstNode node, final Path parentDir) {
    final String entry = node.getTokenValue().trim();
    if (entry.isEmpty()) {
      return;
    }

    final boolean isDirectory = entry.endsWith("/");
    final Path resolvedPath = parentDir.resolve(entry);
    if (isDirectory) {
      this.checkDirectoryEntry(node, entry, resolvedPath);
    } else {
      this.checkFileEntry(node, entry, resolvedPath);
    }
  }

  private void checkDirectoryEntry(final AstNode node, final String entry, final Path path) {
    // Directory entry - should exist and contain a load_list.txt file.
    if (!Files.exists(path)) {
      this.addIssue(node, MESSAGE_DIRECTORY.formatted(entry));
      return;
    }

    if (!Files.isDirectory(path)) {
      this.addIssue(node, MESSAGE_DIRECTORY.formatted(entry));
      return;
    }

    // Check if the directory contains a load_list.txt file.
    final Path nestedLoadList = path.resolve("load_list.txt");
    if (!Files.exists(nestedLoadList)) {
      this.addIssue(node, MESSAGE_NESTED_LOAD_LIST.formatted(entry));
    }
  }

  private void checkFileEntry(final AstNode node, final String entry, final Path path) {
    // File should exist.
    final String fileName = path.getFileName() + ".magik";
    final Path sourceFilePath = path.resolveSibling(fileName);
    if (!Files.exists(sourceFilePath)) {
      this.addIssue(node, MESSAGE_FILE.formatted(entry));
      return;
    }

    if (!Files.isRegularFile(sourceFilePath)) {
      this.addIssue(node, MESSAGE_FILE.formatted(entry));
    }
  }
}
