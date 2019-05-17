package org.stevenlooman.sw.magik.lint.output;

import org.stevenlooman.sw.magik.lint.CheckInfraction;

import java.io.FileNotFoundException;

public abstract class Reporter {

  public abstract void reportIssue(CheckInfraction checkInfraction) throws
      FileNotFoundException;

}
