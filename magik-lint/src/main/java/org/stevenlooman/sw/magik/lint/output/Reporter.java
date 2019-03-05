package org.stevenlooman.sw.magik.lint.output;

import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.lint.CheckInfo;

import java.io.FileNotFoundException;
import java.nio.file.Path;

abstract public class Reporter {

  public abstract void reportIssue(Path path, CheckInfo checkInfo, MagikIssue issue) throws FileNotFoundException;

}
