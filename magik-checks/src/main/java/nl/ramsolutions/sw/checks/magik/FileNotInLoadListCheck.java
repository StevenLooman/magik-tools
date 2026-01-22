package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
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

    final String filename = path.getFileName().toString();
    final String filenameWithoutExtension =
        filename.replaceFirst("[.][^.]+$", ""); // strip .extension
    for (final String line : lines) {
      final String strippedLine = line.split("#")[0].trim();
      if (strippedLine.equals(filename) || strippedLine.equals(filenameWithoutExtension)) {
        return;
      }
    }

    this.addFileIssue(MESSAGE);
  }
}
