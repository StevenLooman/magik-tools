package org.stevenlooman.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MagikFileScanner {

  static Collection<Path> getFilesFromArgs(String[] args) throws IOException {
    List<Path> files = new ArrayList<>();

    for (String arg: args) {
      Path path = Paths.get(arg);
      Collection<Path> filesFromArg = MagikFileScanner.scanMagikFiles(path);
      files.addAll(filesFromArg);
    }

    return files;
  }

  static Path getSingleDirectoryFromArguments(String[] args)
      throws IOException, MagikLintException {
    for (String arg: args) {
      Path path = Paths.get(arg);
      File file = path.toFile();
      if (!file.isDirectory()) {
        throw new MagikLintException("Expected a single directory");
      }

      return path;
    }

    throw new MagikLintException("Expected a single directory");
  }

  static Collection<Path> scanMagikFiles(Path start) throws IOException {
    List<Path> files = new ArrayList<>();

    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attr) {
        if (!path.endsWith(".")
            && path.toFile().isHidden()) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
        if (!attr.isSymbolicLink()
            && path.getFileName().toString().toLowerCase().endsWith(".magik")) {
          files.add(path);
        }
        return FileVisitResult.CONTINUE;
      }
    });

    return files;
  }

}
