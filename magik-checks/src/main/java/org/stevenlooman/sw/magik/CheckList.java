package org.stevenlooman.sw.magik;

import org.stevenlooman.sw.magik.checks.CommentRegularExpressionCheck;
import org.stevenlooman.sw.magik.checks.ExemplarSlotCountCheck;
import org.stevenlooman.sw.magik.checks.FileNotInLoadListCheck;
import org.stevenlooman.sw.magik.checks.LhsRhsComparatorEqualCheck;
import org.stevenlooman.sw.magik.checks.LineLengthCheck;
import org.stevenlooman.sw.magik.checks.MethodComplexityCheck;
import org.stevenlooman.sw.magik.checks.SyntaxErrorCheck;
import org.stevenlooman.sw.magik.checks.SizeZeroEmptyCheck;
import org.stevenlooman.sw.magik.checks.TrailingWhitespaceCheck;
import org.stevenlooman.sw.magik.checks.UnusedVariableCheck;
import org.stevenlooman.sw.magik.checks.VariableNamingCheck;
import org.stevenlooman.sw.magik.checks.XPathCheck;

import java.util.Arrays;
import java.util.List;

public final class CheckList {
  public static final String REPOSITORY_KEY = "magik";
  public static final String PROFILE_DIR = "org/stevenlooman/sw/sonar/l10n/magik/rules";
  public static final String PROFILE_LOCATION = PROFILE_DIR + "/Sonar_way_profile.json";

  private CheckList() {
  }

  /**
   * Get the list of Checks.
   * @return List of with Checks
   */
  public static List<Class<?>> getChecks() {
    return Arrays.asList(
      CommentRegularExpressionCheck.class,
      ExemplarSlotCountCheck.class,
      FileNotInLoadListCheck.class,
      LhsRhsComparatorEqualCheck.class,
      LineLengthCheck.class,
      MethodComplexityCheck.class,
      SyntaxErrorCheck.class,
      SizeZeroEmptyCheck.class,
      TrailingWhitespaceCheck.class,
      UnusedVariableCheck.class,
      VariableNamingCheck.class,
      XPathCheck.class
    );
  }
}
