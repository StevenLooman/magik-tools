package org.stevenlooman.sw.magik.lint.output;

import org.stevenlooman.sw.magik.lint.CheckInfraction;

public abstract class Reporter {

  public abstract void reportIssue(CheckInfraction checkInfraction);

}
