package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.loadlist.LoadListDefinition;
import nl.ramsolutions.sw.loadlist.LoadListFile;
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
    final LoadListFile loadListFile = LoadListFile.getLoadListFileForUri(uri);
    if (loadListFile == null) {
      return;
    }

    final LoadListDefinition loadListDefinition = loadListFile.getLoadListDefinition();
    final List<String> entries = loadListDefinition.getFilePaths();

    // Get the actual filename, with extension.
    final Path path = Path.of(uri);
    final String filename = path.getFileName().toString();
    for (final String entry : entries) {
      // Resolve the entry: if no '.', append '.magik', otherwise use as-is.
      final String resolvedFilename = entry.contains(".") ? entry : entry + ".magik";

      if (resolvedFilename.equals(filename)) {
        return;
      }
    }

    this.addFileIssue(MESSAGE);
  }
}
