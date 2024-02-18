package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if file is in load_list.txt. */
@Rule(key = FileNotInLoadListCheck.CHECK_KEY)
public class FileNotInLoadListCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "FileNotInLoadList";

  private static final String MESSAGE = "File is not included in load_list.";

  @Override
  protected void walkPreMagik(final AstNode node) {
    final URI uri = this.getMagikFile().getUri();
    final Path path = Path.of(uri);
    if (path == null) {
      return;
    }

    final Path loadListPath = path.resolveSibling("load_list.txt");
    final File loadListFile = loadListPath.toFile();
    if (!loadListFile.exists()) {
      return;
    }

    final List<String> lines;
    try {
      lines = Files.readAllLines(loadListPath);
    } catch (IOException ex) {
      // silently ignore this
      return;
    }

    // strip .extension
    final String filename = path.getFileName().toString().replaceFirst("[.][^.]+$", "");
    for (final String line : lines) {
      if (line.trim().equals(filename)) {
        return;
      }
    }

    this.addFileIssue(MESSAGE);
  }
}
