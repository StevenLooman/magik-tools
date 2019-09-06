package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@Rule(key = FileNotInLoadListCheck.CHECK_KEY)
public class FileNotInLoadListCheck extends MagikCheck {

  private static final String MESSAGE = "File is not included in load_list.";
  public static final String CHECK_KEY = "FileNotInLoadList";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Collections.emptyList();
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    Path path = getContext().path();
    if (path == null) {
      return;
    }

    Path loadListPath = path.resolveSibling("load_list.txt");
    File loadListFile = loadListPath.toFile();
    if (!loadListFile.exists()) {
      return;
    }

    List<String> lines;
    try {
      lines = Files.readAllLines(loadListPath);
    } catch (IOException ex) {
      // silently ignore this
      return;
    }

    String filename = path.getFileName().toString();
    filename = filename.replaceFirst("[.][^.]+$", "");  // strip .extension
    for (String line: lines) {
      if (line.trim().equals(filename)) {
        return;
      }
    }

    addFileIssue(MESSAGE);
  }

}
